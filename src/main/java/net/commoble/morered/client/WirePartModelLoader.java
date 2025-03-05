package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.commoble.morered.client.WirePartModelLoader.WirePartGeometry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ExtendedUnbakedModel;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class WirePartModelLoader implements UnbakedModelLoader<WirePartGeometry>
{
	public static final WirePartModelLoader INSTANCE = new WirePartModelLoader();

	@Override
	public WirePartModelLoader.WirePartGeometry read(JsonObject modelJson, JsonDeserializationContext context)
	{
        BlockModel lineModel = context.deserialize(GsonHelper.getAsJsonObject(modelJson, "line"), BlockModel.class);
        BlockModel edgeModel = context.deserialize(GsonHelper.getAsJsonObject(modelJson, "edge"), BlockModel.class);
        return new WirePartGeometry(lineModel, edgeModel);
	}
	
	public static record WirePartGeometry(BlockModel lineModel, BlockModel edgeModel) implements ExtendedUnbakedModel
	{
		@Override
		public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState modelState, boolean useAmbientOcclusion, boolean usesBlockLight, ItemTransforms itemTransforms, ContextMap additionalProperties)
		{
			BakedModel[] lineModels = new BakedModel[24];
			BakedModel[] edgeModels = new BakedModel[12];
			
			// for each combination of attachment face + rotated connection side, bake a connection line model
			for (int side = 0; side < 6; side++)
			{
				for (int subSide = 0; subSide < 4; subSide++)
				{
					int index = side*4 + subSide;
					ModelState transform = FaceRotation.getFaceRotation(side, subSide);
					lineModels[index] = UnbakedModel.bakeWithTopModelValues(this.lineModel, baker, transform);
				}
			}
			
			for (int edge = 0; edge < 12; edge++)
			{
				// the 12 edges are a little weirder to index
				// let's define them in directional precedence
				// down comes first, then up, then the sides
				// the "default" edge with no rotation has to be on the middle sides to ignore the z-axis, we'll use bottom-west
				ModelState transform = EdgeRotation.EDGE_ROTATIONS[edge];
				edgeModels[edge] = UnbakedModel.bakeWithTopModelValues(this.edgeModel, baker, transform);
			}
			
			return new WirePartModelLoader.WirePartModel(useAmbientOcclusion, usesBlockLight,
				baker.findSprite(UnbakedModel.getTopTextureSlots(this.lineModel, baker.rootName()), "particle"),
				lineModels,
				edgeModels);
		}

		@Override
		public void resolveDependencies(Resolver resolver)
		{
			this.lineModel.resolveDependencies(resolver);
			this.edgeModel.resolveDependencies(resolver);
		}
	}
	
	public static class WirePartModel implements IDynamicBakedModel
	{
		private static final List<BakedQuad> NO_QUADS = ImmutableList.of();
		
        private final boolean isAmbientOcclusion;
        private final boolean isSideLit;
        private final TextureAtlasSprite particle;
        private final BakedModel[] lineModels;
        private final BakedModel[] edgeModels;
		
		public WirePartModel(boolean isAmbientOcclusion, boolean isSideLit, TextureAtlasSprite particle, BakedModel[] lineModels, BakedModel[] edgeModels)
		{
			this.isAmbientOcclusion = isAmbientOcclusion;
			this.isSideLit = isSideLit;
			this.particle = particle;
			this.lineModels = lineModels;
			this.edgeModels = edgeModels;
		}

		@Override
		public boolean useAmbientOcclusion()
		{
			return this.isAmbientOcclusion;
		}

		@Override
		public boolean isGui3d()
		{
			return true;
		}

		@Override
		public boolean usesBlockLight()
		{
			return this.isSideLit;
		}

		@Override
		public TextureAtlasSprite getParticleIcon()
		{
			return this.particle;
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType)
		{
			Long wireData = extraData.get(WireModelData.PROPERTY);
			if (wireData == null || wireData == 0)
				return NO_QUADS;
			long wireFlags = wireData;

			List<BakedQuad> quads = new ArrayList<>();
			int lineStart = 6;
			int lines = 24;
			int edgeStart = lineStart+lines;
			int edges = 12;
			for (int i=0; i<lines; i++)
			{
				if (WireModelData.test(wireFlags, i+lineStart))
					quads.addAll(this.lineModels[i].getQuads(state, side, rand, extraData, renderType));
			}
			for (int i=0; i < edges; i++)
			{
				if (WireModelData.test(wireFlags, i+edgeStart))
					quads.addAll(this.edgeModels[i].getQuads(state, side, rand, extraData, renderType));
			}
			
			return quads;
		}

		@Override
		public ItemTransforms getTransforms()
		{
			return ItemTransforms.NO_TRANSFORMS; // wire parts are not rotated by the blockstate file, and the submodels have their own transforms
		}		
	}
	
	public static final class WireModelData
	{
		private WireModelData() {}; // static member holder
		
		public static final ModelProperty<Long> PROPERTY = new ModelProperty<>();

		/**
		 * The flags here are a bit pattern indicating a wire block's state and neighbor context
		 * first 6 bits [0-5] -- which interior faces the wire block has true wires attached to from blockitems
		 * 	the bits use the directional index, e.g. bit 0 = DOWN
		 * next 24 bits [6-29] -- indicates the connecting lines that should be rendered
		 * 	these use the 24 FaceRotation indices
		 * next 12 bits [30-41] -- indicates which connecting edges that should be rendered
		 * 	these use the 12 EdgeRotation indeces
		 */
		public static boolean test(long flags, int bitIndex)
		{
			return ((1L << bitIndex) & flags) != 0;
		}
	}
}