package commoble.morered.datagen;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SimpleModel {
    public static final Codec<SimpleModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("parent").forGetter(SimpleModel::getParent),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("textures", ImmutableMap.of()).forGetter(SimpleModel::getTextures)
    ).apply(instance, SimpleModel::new));

    private final String parent;

    public String getParent() {
        return this.parent;
    }

    private final Map<String, String> textures;

    public Map<String, String> getTextures() {
        return this.textures;
    }

    public static SimpleModel builder(String parent) {
        return new SimpleModel(parent, new HashMap<>());
    }

    public SimpleModel(String parent, Map<String, String> textures) {
        this.parent = parent;
        this.textures = textures;
    }

    public SimpleModel withTexture(String textureName, String textureID) {
        this.textures.put(textureName, textureID);
        return this;
    }
}
