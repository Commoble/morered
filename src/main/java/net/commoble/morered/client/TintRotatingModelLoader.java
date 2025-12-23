package net.commoble.morered.client;

import org.joml.Matrix4fc;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.commoble.morered.wires.Edge;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

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
public class TintRotatingModelLoader implements UnbakedModelLoader<BlockModel>
{
	public static final TintRotatingModelLoader INSTANCE = new TintRotatingModelLoader();

	@Override
	public BlockModel read(JsonObject modelContents, JsonDeserializationContext context)
	{
		// we use the vanilla model loader to parse everything
        BlockModel baseModel = context.deserialize(modelContents.get("model"), BlockModel.class);
        TintRotatingModelGeometry geometry = new TintRotatingModelGeometry(baseModel.geometry());
        return new BlockModel(
			geometry,
			baseModel.guiLight(),
			baseModel.ambientOcclusion(),
			baseModel.transforms(),
			baseModel.textureSlots(),
			baseModel.parent(),
			baseModel.rootTransform(),
			baseModel.renderTypeGroup(),
			baseModel.partVisibility());
	}
	
	public static class TintRotatingModelGeometry implements ExtendedUnbakedGeometry
	{
		private final UnbakedGeometry baseGeometry;
		
		public TintRotatingModelGeometry(UnbakedGeometry baseGeometry)
		{
			this.baseGeometry = baseGeometry;
		}
			
		@Override
		public QuadCollection bake(TextureSlots textures, ModelBaker baker, ModelState modelState, ModelDebugName modelDebugName, ContextMap additionalProperties)
		{
			QuadCollection baseQuads = this.baseGeometry.bake(textures,baker,modelState,modelDebugName, additionalProperties);
	        QuadCollection.Builder builder = new QuadCollection.Builder();

			Matrix4fc rotation = modelState.transformation().getMatrix();
			for (Direction dir : Direction.values())
			{ 
				for (BakedQuad quad : baseQuads.getQuads(dir))
				{
					builder.addCulledFace(dir, getTintRotatedQuad(quad, rotation));
				}
			}
			for (BakedQuad quad : baseQuads.getQuads(null))
			{
				builder.addUnculledFace(getTintRotatedQuad(quad, rotation));
			}
			return builder.build();
		}

		protected BakedQuad getTintRotatedQuad(BakedQuad baseQuad, Matrix4fc rotation)
		{
			int newTint = this.rotateTint(baseQuad.tintIndex(), rotation);
			return new BakedQuad(
				baseQuad.position0(),
				baseQuad.position1(),
				baseQuad.position2(),
				baseQuad.position3(),
				baseQuad.packedUV0(),
				baseQuad.packedUV1(),
				baseQuad.packedUV2(),
				baseQuad.packedUV3(),
				newTint,
				baseQuad.direction(),
				baseQuad.sprite(),
				baseQuad.shade(),
				baseQuad.lightEmission(),
				baseQuad.bakedNormals(),
				baseQuad.bakedColors(),
				baseQuad.hasAmbientOcclusion());
		}
		
		protected int rotateTint(int baseTint, Matrix4fc rotation)
		{
			return baseTint < 1 ? baseTint
				: baseTint < 7 ? this.rotateSide(baseTint, rotation)
				: baseTint < 19 ? this.rotateEdge(baseTint, rotation)
				: baseTint;
		}
		
		protected int rotateSide(int baseTint, Matrix4fc rotation)
		{
			int ordinal = baseTint - 1;
			Direction baseDir = Direction.from3DDataValue(ordinal);
			Direction newDir = Direction.rotate(rotation, baseDir);
			return newDir.ordinal() + 1;
		}
		
		protected int rotateEdge(int baseTint, Matrix4fc rotation)
		{
			int ordinal = baseTint - 7;
			
			return EdgeRotation.getRotatedEdge(Edge.values()[ordinal], rotation).ordinal() + 7;
		}
	}
}
