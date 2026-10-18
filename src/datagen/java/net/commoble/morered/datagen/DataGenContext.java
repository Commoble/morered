package net.commoble.morered.datagen;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import net.commoble.exmachina.api.MechanicalComponent;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.LanguageProvider;

public record DataGenContext(
	Map<Identifier, MechanicalComponent> mechanicalComponents,
	Map<Identifier, BlockStateModelDispatcher> blockStates,
	Map<Identifier, ClientItem> clientItems,
	Map<Identifier, SimpleModel> models,
	Map<Identifier, PreviewModel> previewModels,
	Map<Identifier, Map<Block, Map<String,Variant>>> placementPreviews,
	Map<Identifier, Function<BootstrapContext<Recipe<?>>, Recipe<?>>> recipes,
	Map<Identifier, Function<BootstrapContext<LootTable>, LootTable>> lootTables,
	Map<Identifier, Function<BootstrapContext<Advancement>, Advancement>> advancements,
	List<Consumer<DataMapsProvider>> dataMaps,
	List<Consumer<TagProvider<Block>>> blockTags,
	List<Consumer<TagProvider<Item>>> itemTags,
	List<Consumer<LanguageProvider>> lang)
{
}
