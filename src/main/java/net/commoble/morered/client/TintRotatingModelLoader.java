package net.commoble.morered.client;

import java.util.List;
import java.util.function.Function;

import org.joml.Matrix4f;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.commoble.morered.client.TintRotatingModelLoader.TintRotatingModelGeometry;
import net.commoble.morered.wires.Edge;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel.Resolver;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

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
public class TintRotatingModelLoader implements IGeometryLoader<TintRotatingModelGeometry>
{
	public static final TintRotatingModelLoader INSTANCE = new TintRotatingModelLoader();

	@Override
	public TintRotatingModelGeometry read(JsonObject modelContents, JsonDeserializationContext context)
	{
		// we use the vanilla model loader to parse everything
//		ElementsModel proxy = ElementsModel.Loader.INSTANCE.read(modelContents, context);
        BlockModel proxy = context.deserialize(modelContents.get("model"), BlockModel.class);
		
        return new TintRotatingModelGeometry(proxy);
	}
	
	public static class TintRotatingModelGeometry implements IUnbakedGeometry<TintRotatingModelGeometry>
	{
		private final BlockModel blockModel;
		
		public TintRotatingModelGeometry(BlockModel proxy)
		{
			this.blockModel = proxy;
		}
			
		@SuppressWarnings("deprecation")
		@Override
		public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
			List<ItemOverride> overrides)
		{
			BakedModel baseModel = blockModel.bake(spriteGetter, modelState, false);
			SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(blockModel, false)
				.particle(spriteGetter.apply(blockModel.getMaterial("particle")));
			Matrix4f rotation = modelState.getRotation().getMatrix();
			for (Direction dir : Direction.values())
			{
				// don't worry about deprecated getQuads, the basemodel doesn't use modeldata and we don't have modeldata in context anyway 
				for (BakedQuad quad : baseModel.getQuads(null, dir, null))
				{
					builder.addCulledFace(dir, getTintRotatedQuad(quad, rotation));
				}
			}
			for (BakedQuad quad : baseModel.getQuads(null, null, null))
			{
				builder.addUnculledFace(getTintRotatedQuad(quad, rotation));
			}
			return builder.build();
		}

		@Override
		public void resolveDependencies(Resolver resolver, IGeometryBakingContext owner)
		{
			this.blockModel.resolveDependencies(resolver);
		}
		
		protected BakedQuad getTintRotatedQuad(BakedQuad baseQuad, Matrix4f rotation)
		{
			int newTint = this.rotateTint(baseQuad.getTintIndex(), rotation);
			return new BakedQuad(baseQuad.getVertices(), newTint, baseQuad.getDirection(), baseQuad.getSprite(), baseQuad.isShade(), baseQuad.getLightEmission(), baseQuad.hasAmbientOcclusion());
		}
		
		protected int rotateTint(int baseTint, Matrix4f rotation)
		{
			return baseTint < 1 ? baseTint
				: baseTint < 7 ? this.rotateSide(baseTint, rotation)
				: baseTint < 19 ? this.rotateEdge(baseTint, rotation)
				: baseTint;
		}
		
		protected int rotateSide(int baseTint, Matrix4f rotation)
		{
			int ordinal = baseTint - 1;
			Direction baseDir = Direction.from3DDataValue(ordinal);
			Direction newDir = Direction.rotate(rotation, baseDir);
			return newDir.ordinal() + 1;
		}
		
		protected int rotateEdge(int baseTint, Matrix4f rotation)
		{
			int ordinal = baseTint - 7;
			
			return EdgeRotation.getRotatedEdge(Edge.values()[ordinal], rotation).ordinal() + 7;
		}
	}
}
