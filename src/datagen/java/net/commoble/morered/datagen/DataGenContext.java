package net.commoble.morered.datagen;

import java.util.Map;

import net.commoble.exmachina.api.MechanicalComponent;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.LanguageProvider;

public record DataGenContext(
	Map<Identifier, MechanicalComponent> mechanicalComponents,
	Map<Identifier, BlockModelDefinition> blockStates,
	Map<Identifier, ClientItem> clientItems,
	Map<Identifier, SimpleModel> models,
	Map<Identifier, PreviewModel> previewModels,
	Map<Identifier, Map<Block, Map<String,Variant>>> placementPreviews,
	Map<Identifier, Recipe<?>> recipes,
	Map<Identifier, LootTable> lootTables,
	DataMapsProvider dataMaps,
	TagProvider<Block> blockTags,
	TagProvider<Item> itemTags,
	LanguageProvider lang)
{
}
