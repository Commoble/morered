package commoble.morered.client;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import commoble.morered.client.TintRotatingModelLoader.TintRotatingModelGeometry;
import commoble.morered.wires.Edge;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelLoaderRegistry.VanillaProxy;
import net.minecraftforge.client.model.geometry.ISimpleModelGeometry;

/**
 model loader that "rotates" a tintindex in a predefined way
 we have wire blocks where we want parts of the wire models to be tinted based on the power
 of the wire
 but different parts of the wire have different power values
 we define a custom model loader here to alter the canonical tintindexes based on the way
 we rotate the base models so they draw power data from the correct attachment faces of
 the wire blocks
 
 tintindex 0 is reserved for particles
 the next six indexes 1-6 indicate a face in dunswe order (down, up, north, south, west east)
 	// this is the wire face we read power data from, not the face of the quad
 the next 12 indexes 7-18 indicate convex edges -- these use the power values of two adjacent wires
 to determine their color (consult Edge or EdgeRotations for edge indices)
 
 in the actual model jsons, we will assign tintindexes to quads depending on their
 position in the canonical model
 we will then sneakily replace the quads when we bake them, updating the tintindexes
 based on the model transform to more appropriate values
 */
public class TintRotatingModelLoader implements IModelLoader<TintRotatingModelGeometry>
{
	public static final TintRotatingModelLoader INSTANCE = new TintRotatingModelLoader();
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager)
	{
		// noop
	}

	@Override
	public TintRotatingModelGeometry read(JsonDeserializationContext context, JsonObject modelContents)
	{
		VanillaProxy proxy = ModelLoaderRegistry.VanillaProxy.Loader.INSTANCE.read(context, modelContents);
        return new TintRotatingModelGeometry(proxy);
	}
	
	public static class TintRotatingModelGeometry implements ISimpleModelGeometry<TintRotatingModelGeometry>
	{
		private final VanillaProxy proxy;
		
		public TintRotatingModelGeometry(VanillaProxy proxy)
		{
			this.proxy = proxy;
		}

		@Override
		public void addQuads(IModelConfiguration owner, IModelBuilder<?> modelBuilder, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter,
			IModelTransform modelTransform, ResourceLocation modelLocation)
		{
			IModelBuilder<?> builderWrapper = new TintRotatingModelBuilder(modelBuilder, modelTransform);
			this.proxy.addQuads(owner, builderWrapper, bakery, spriteGetter, modelTransform, modelLocation);
		}

		@Override
		public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter,
			Set<Pair<String, String>> missingTextureErrors)
		{
			return this.proxy.getTextures(owner, modelGetter, missingTextureErrors);
		}
	}
	
	public static class TintRotatingModelBuilder implements IModelBuilder<TintRotatingModelBuilder>
	{
		private final IModelBuilder<?> delegate;
		private final Matrix4f rotation;
		
		public TintRotatingModelBuilder(IModelBuilder<?> delegate, IModelTransform modelTransform)
		{
			this.delegate = delegate;
			this.rotation = modelTransform.getRotation().getMatrix();
		}

		@Override
		public TintRotatingModelBuilder addFaceQuad(Direction facing, BakedQuad quad)
		{
			BakedQuad tintRotatedQuad = this.getTintRotatedQuad(quad);
			this.delegate.addFaceQuad(facing,tintRotatedQuad);
			return this;
		}

		@Override
		public TintRotatingModelBuilder addGeneralQuad(BakedQuad quad)
		{
			BakedQuad tintRotatedQuad = this.getTintRotatedQuad(quad);
			this.delegate.addGeneralQuad(tintRotatedQuad);
			return this;
		}

		@Override
		public IBakedModel build()
		{
			return this.delegate.build();
		}
		
		protected BakedQuad getTintRotatedQuad(BakedQuad baseQuad)
		{
			int newTint = this.rotateTint(baseQuad.getTintIndex());
			return new BakedQuad(baseQuad.getVertices(), newTint, baseQuad.getDirection(), baseQuad.getSprite(), baseQuad.isShade());
		}
		
		protected int rotateTint(int baseTint)
		{
			return baseTint < 1 ? baseTint
				: baseTint < 7 ? this.rotateSide(baseTint)
				: baseTint < 19 ? this.rotateEdge(baseTint)
				: baseTint;
		}
		
		protected int rotateSide(int baseTint)
		{
			int ordinal = baseTint - 1;
			Direction baseDir = Direction.from3DDataValue(ordinal);
			Direction newDir = Direction.rotate(this.rotation, baseDir);
			return newDir.ordinal() + 1;
		}
		
		protected int rotateEdge(int baseTint)
		{
			int ordinal = baseTint - 7;
			
			return EdgeRotation.getRotatedEdge(Edge.values()[ordinal], this.rotation).ordinal() + 7;
		}
	}
}
