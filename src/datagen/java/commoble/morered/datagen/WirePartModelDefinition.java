package commoble.morered.datagen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WirePartModelDefinition
{
	public static final Codec<WirePartModelDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("loader").forGetter($->"morered:wire_parts"),
			SimpleModel.CODEC.fieldOf("line").forGetter(WirePartModelDefinition::getLine),
			SimpleModel.CODEC.fieldOf("edge").forGetter(WirePartModelDefinition::getEdge)
		).apply(instance, ($, line, edge) -> new WirePartModelDefinition(line,edge)));

	private final SimpleModel line;	public SimpleModel getLine() { return this.line; }
	private final SimpleModel edge;	public SimpleModel getEdge() { return this.edge; }

	public WirePartModelDefinition(SimpleModel line, SimpleModel edge)
	{
		this.line = line;
		this.edge = edge;
	}
}
