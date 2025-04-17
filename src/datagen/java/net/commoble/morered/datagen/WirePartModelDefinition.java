package net.commoble.morered.datagen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WirePartModelDefinition(SimpleModel line, SimpleModel edge)
{
	public static final Codec<WirePartModelDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("loader").forGetter($->"morered:wire_parts"),
			SimpleModel.CODEC.fieldOf("line").forGetter(WirePartModelDefinition::line),
			SimpleModel.CODEC.fieldOf("edge").forGetter(WirePartModelDefinition::edge)
		).apply(instance, ($, line, edge) -> new WirePartModelDefinition(line,edge)));
}
