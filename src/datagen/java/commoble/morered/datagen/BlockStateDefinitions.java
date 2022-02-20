package commoble.morered.datagen;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BlockStateDefinitions {
    /**
     * Creates and returns a VariantBlockStateDefinition for whom variants can be defined.
     * Once variants are added to the definition, giving this to a JsonDataProvider
     * is sufficient to cause the blockstate jsons to be generated, so long as that data provider
     * is given to the data generator, or if that data provider's act method is called by another acting
     * data provider's act method.
     *
     * @return a mutable VariantBlockStateDefinition
     */
    public static VariantBlockStateDefinition variantBuilder() {
        return new VariantBlockStateDefinition(new HashMap<>());
    }

    /**
     * Creates and returns a MultipartBlockStateDefinition for whom cases can be defined.
     * Once cases are added to the definition, giving this to a JsonDataProvider
     * is sufficient to cause the blockstate jsons to be generated, so long as that data provider
     * is given to the data generator, or if that data provider's act method is called by another acting
     * data provider's act method.
     *
     * @return a mutable MultipartBlockStateDefinition
     */
    public static MultipartBlockStateDefinition multipartBuilder() {
        return new MultipartBlockStateDefinition(new ArrayList<>());
    }

    public static class VariantBlockStateDefinition {
        public static final Codec<VariantBlockStateDefinition> CODEC = Codec.unboundedMap(Codec.STRING,
                        PartDefinition.CODEC)
                .xmap(VariantBlockStateDefinition::new, VariantBlockStateDefinition::getVariants)
                .fieldOf("variants").codec();

        private final Map<String, PartDefinition> variants;

        public Map<String, PartDefinition> getVariants() {
            return this.variants;
        }

        public VariantBlockStateDefinition(Map<String, PartDefinition> variants) {
            this.variants = variants;
        }

        public VariantBlockStateDefinition withVariant(String variant, PartDefinition part) {
            this.variants.put(variant, part);
            return this;
        }
    }

    public static class MultipartBlockStateDefinition {
        public static final Codec<MultipartBlockStateDefinition> CODEC = CaseDefinition.CODEC.listOf()
                .xmap(MultipartBlockStateDefinition::new, MultipartBlockStateDefinition::getParts)
                .fieldOf("multipart").codec();
        private final List<CaseDefinition> cases;

        public List<CaseDefinition> getParts() {
            return this.cases;
        }

        public MultipartBlockStateDefinition(List<CaseDefinition> cases) {
            this.cases = cases;
        }

        public MultipartBlockStateDefinition withCase(CaseDefinition newCase) {
            this.cases.add(newCase);
            return this;
        }

        public static class CaseDefinition {
            public static final Codec<CaseDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("when", ImmutableMap.of()).forGetter(CaseDefinition::getWhen),
                    PartDefinition.CODEC.fieldOf("apply").forGetter(CaseDefinition::getApply)
            ).apply(instance, CaseDefinition::new));

            private final Map<String, String> when;

            public Map<String, String> getWhen() {
                return this.when;
            }

            private final PartDefinition apply;

            public PartDefinition getApply() {
                return this.apply;
            }

            public static CaseDefinition builder(PartDefinition model) {
                return new CaseDefinition(new HashMap<>(), model);
            }

            public static CaseDefinition builder(ResourceLocation model, int x, int y) {
                return CaseDefinition.builder(new PartDefinition(model, x, y));
            }

            public CaseDefinition(Map<String, String> when, PartDefinition apply) {
                this.when = when;
                this.apply = apply;
            }

            public CaseDefinition withCondition(String condition, String value) {
                this.when.put(condition, value);
                return this;
            }

        }
    }

    public static class PartDefinition {
        public static final Codec<PartDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(PartDefinition::getModel),
                Codec.INT.optionalFieldOf("x", 0).forGetter(PartDefinition::getX),
                Codec.INT.optionalFieldOf("y", 0).forGetter(PartDefinition::getY),
                Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(PartDefinition::getUvlock),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(PartDefinition::getWeight)
        ).apply(instance, PartDefinition::new));

        private final ResourceLocation model;

        public ResourceLocation getModel() {
            return this.model;
        }

        private final int x;

        public int getX() {
            return this.x;
        }

        private final int y;

        public int getY() {
            return this.y;
        }

        private final boolean uvlock;

        public boolean getUvlock() {
            return this.uvlock;
        }

        private final int weight;

        public int getWeight() {
            return this.weight;
        }

        public PartDefinition(ResourceLocation model, int x, int y) {
            this(model, x, y, false, 1);
        }

        public PartDefinition(ResourceLocation model, int x, int y, boolean uvlock, int weight) {
            this.model = model;
            this.x = x;
            this.y = y;
            this.uvlock = uvlock;
            this.weight = weight;
        }
    }
}
