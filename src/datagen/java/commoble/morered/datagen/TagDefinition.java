package commoble.morered.datagen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class TagDefinition {
    public static final Codec<TagDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(TagDefinition::getReplace),
            Codec.STRING.listOf().fieldOf("values").forGetter(TagDefinition::getValues)
    ).apply(instance, TagDefinition::new));

    private final boolean replace;

    public boolean getReplace() {
        return this.replace;
    }

    private final List<String> values;

    public List<String> getValues() {
        return this.values;
    }

    public static TagDefinition builder() {
        return builder(false);
    }

    public static TagDefinition builder(boolean replace) {
        return new TagDefinition(replace, new ArrayList<>());
    }

    public TagDefinition(boolean replace, List<String> values) {
        this.replace = replace;
        this.values = values;
    }

    public TagDefinition with(String value) {
        this.values.add(value);
        return this;
    }

    public TagDefinition withObject(ResourceLocation objectID) {
        return this.with(objectID.toString());
    }

    public TagDefinition withObject(IForgeRegistryEntry<?> object) {
        return this.withObject(object.getRegistryName());
    }

    public TagDefinition withTag(String tagName) {
        return this.with("#" + tagName);
    }

    public TagDefinition withTag(ResourceLocation tagName) {
        return this.withTag(tagName.toString());
    }

    public TagDefinition withTag(Tag.Named<?> tag) {
        return this.withTag(tag.getName());
    }
}
