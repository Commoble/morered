package commoble.morered.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongPredicate;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import commoble.morered.client.WirePartModelLoader.WirePartGeometry;
import commoble.morered.wires.AbstractWireBlock;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IModelGeometry;

public class WirePartModelLoader implements IModelLoader<WirePartGeometry> {
    public static final WirePartModelLoader INSTANCE = new WirePartModelLoader();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        // not needed at the moment, consider using if caches need to be cleared
    }

    @Override
    public WirePartModelLoader.WirePartGeometry read(JsonDeserializationContext context, JsonObject modelContents) {
        BlockModel lineModel = context.deserialize(GsonHelper.getAsJsonObject(modelContents, "line"), BlockModel.class);
        BlockModel edgeModel = context.deserialize(GsonHelper.getAsJsonObject(modelContents, "edge"), BlockModel.class);
        return new WirePartGeometry(lineModel, edgeModel);
    }

    public static class WirePartGeometry implements IModelGeometry<WirePartGeometry> {
        private final BlockModel lineModel;
        private final BlockModel edgeModel;

        public WirePartGeometry(BlockModel lineModel, BlockModel edgeModel) {
            this.lineModel = lineModel;
            this.edgeModel = edgeModel;
        }

        @Override
        public BakedModel bake(IModelConfiguration owner, ModelBakery bakery,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
            BakedModel[] lineModels = new BakedModel[24];
            BakedModel[] edgeModels = new BakedModel[12];
            boolean isSideLit = owner.isSideLit();

            // for each combination of attachment face + rotated connection side, bake a connection line model
            for (int side = 0; side < 6; side++) {
                for (int subSide = 0; subSide < 4; subSide++) {
                    int index = side * 4 + subSide;
                    ModelState transform = FaceRotation.getFaceRotation(side, subSide);
                    lineModels[index] = this.lineModel.bake(bakery, this.lineModel, spriteGetter, transform,
                            modelLocation, isSideLit);
                }
            }

            for (int edge = 0; edge < 12; edge++) {
                // the 12 edges are a little weirder to index
                // let's define them in directional precedence
                // down comes first, then up, then the sides
                // the "default" edge with no rotation has to be on the middle sides to ignore the z-axis, we'll use
                // bottom-west
                ModelState transform = EdgeRotation.EDGE_ROTATIONS[edge];
                edgeModels[edge] = this.edgeModel.bake(bakery, this.edgeModel, spriteGetter, transform, modelLocation
                        , isSideLit);
            }

            return new WirePartModelLoader.WirePartModel(owner.useSmoothLighting(), owner.isShadedInGui(),
                    owner.isSideLit(),
                    spriteGetter.apply(this.lineModel.getMaterial("particle")), lineModels, edgeModels);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner,
                                                Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String
                , String>> missingTextureErrors) {
            Set<Material> textures = new HashSet<>();
            textures.addAll(this.lineModel.getMaterials(modelGetter, missingTextureErrors));
            textures.addAll(this.edgeModel.getMaterials(modelGetter, missingTextureErrors));
            return textures;
        }
    }

    public static class WirePartModel implements IDynamicBakedModel {
        private static final List<BakedQuad> NO_QUADS = ImmutableList.of();

        private final boolean isAmbientOcclusion;
        private final boolean isGui3d;
        private final boolean isSideLit;
        private final TextureAtlasSprite particle;
        private final BakedModel[] lineModels;
        private final BakedModel[] edgeModels;

        public WirePartModel(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit,
                             TextureAtlasSprite particle, BakedModel[] lineModels, BakedModel[] edgeModels) {
            this.isAmbientOcclusion = isAmbientOcclusion;
            this.isGui3d = isGui3d;
            this.isSideLit = isSideLit;
            this.particle = particle;
            this.lineModels = lineModels;
            this.edgeModels = edgeModels;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.isAmbientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return this.isGui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return this.isSideLit;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.particle;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData) {
            WireModelData wireData = extraData.getData(WireModelData.PROPERTY);
            if (wireData == null)
                return NO_QUADS;

            List<BakedQuad> quads = new ArrayList<>();
            int lineStart = 6;
            int lines = 24;
            int edgeStart = lineStart + lines;
            int edges = 12;
            for (int i = 0; i < lines; i++) {
                if (wireData.test(i + lineStart))
                    quads.addAll(this.lineModels[i].getQuads(state, side, rand, extraData));
            }
            for (int i = 0; i < edges; i++) {
                if (wireData.test(i + edgeStart))
                    quads.addAll(this.edgeModels[i].getQuads(state, side, rand, extraData));
            }

            return quads;
        }

        @Override
        public IModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileData) {
            Block block = state.getBlock();
            if (block instanceof AbstractWireBlock) {
                return new WirePartModelLoader.WireModelData(((AbstractWireBlock) block).getExpandedShapeIndex(state,
                        world, pos));
            } else {
                return tileData;
            }
        }


    }

    public static class WireModelData implements IModelData, LongPredicate {
        public static final ModelProperty<WireModelData> PROPERTY = new ModelProperty<>();

        /**
         * The flags here are a bit pattern indicating a wire block's state and neighbor context
         * first 6 bits [0-5] -- which interior faces the wire block has true wires attached to from blockitems
         * the bits use the directional index, e.g. bit 0 = DOWN
         * next 24 bits [6-29] -- indicates the connecting lines that should be rendered
         * these use the 24 FaceRotation indices
         * next 12 bits [30-41] -- indicates which connecting edges that should be rendered
         * these use the 12 EdgeRotation indeces
         */
        private final long flags;

        public WireModelData(long flags) {
            this.flags = flags;
        }

        @Override
        public boolean test(long i) {
            return ((1L << i) & this.flags) != 0;
        }

        @Override
        public boolean hasProperty(ModelProperty<?> prop) {
            return prop == PROPERTY;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getData(ModelProperty<T> prop) {
            if (prop == PROPERTY)
                return (T) this;
            else
                return null;
        }

        @Override
        public <T> T setData(ModelProperty<T> prop, T data) {
            return data;
        }

    }

}