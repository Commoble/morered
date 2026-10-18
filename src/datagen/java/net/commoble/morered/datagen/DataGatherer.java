package net.commoble.morered.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.commoble.morered.MoreRed;
import net.commoble.preview_placement.client.PlacementPreviewDefinition;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public record DataGatherer(Supplier<DataGenContext> memoizedContext) implements Consumer<GatherDataEvent.Client> 
{

	@Override
	public void accept(GatherDataEvent.Client event)
	{
		DataGenContext context = this.memoizedContext.get();
		
		DataGenerator generator = event.getGenerator();
		CompletableFuture<HolderLookup.Provider> holders = event.getWorldLookupProvider();
		PackOutput output = generator.getPackOutput();
		
		DataMapsProvider dataMaps = new DataMapsProvider(output, holders);
		TagProvider<Block> blockTags = TagProvider.create(event, Registries.BLOCK, holders);
		TagProvider<Item> itemTags = TagProvider.create(event, Registries.ITEM, holders);
		LanguageProvider lang = new LanguageProvider(output, MoreRed.MODID, "en_us")
		{
			@Override
			protected void addTranslations()
			{} // no
		};
		
		context.blockTags().forEach(consumer -> consumer.accept(blockTags));
		context.itemTags().forEach(consumer -> consumer.accept(itemTags));
		
		BlockStateBuilder.addDataProvider(event, context.blockStates());
		JsonDataProvider.addProvider(event, PackOutput.Target.RESOURCE_PACK, "items", ClientItem.CODEC, context.clientItems());
		JsonDataProvider.addProvider(event, PackOutput.Target.RESOURCE_PACK, "models", SimpleModel.CODEC, context.models());
		PreviewModel.addDataProvider(event, context.previewModels());
		PlacementPreviewDefinition.addDataProvider(event, context.placementPreviews());

		generator.addProvider(true, lang);
		generator.addProvider(true, dataMaps);
		generator.addProvider(true, blockTags);
		generator.addProvider(true, itemTags);
	}

}
