package net.commoble.morered.datagen;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public record PreviewModel(SimpleModel model)
{
	public static final Codec<PreviewModel> CODEC = RecordCodecBuilder.create(builder -> builder.group(
		Codec.STRING.fieldOf("loader").forGetter($ -> "preview_placement:placement_preview"),
		SimpleModel.CODEC.fieldOf("model").forGetter(PreviewModel::model)
	).apply(builder, ($,model) -> new PreviewModel(model)));
	
	public static void addDataProvider(GatherDataEvent event, Map<Identifier, PreviewModel> models)
	{
		JsonDataProvider.addNamed(event, Target.RESOURCE_PACK, "models", CODEC, models, "Placement Preview Models");
	}
}
