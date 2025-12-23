package net.commoble.morered.datagen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import net.commoble.morered.MoreRed;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

// advancements are totally unrelated to everything else
// so I don't mind doing them in a separate class
public class MoreRedAdvancements
{
	public static void genAdvancements(GatherDataEvent event, LanguageProvider lang)
	{
		event.addProvider(new AdvancementProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(), List.of((registries, advancements) -> {
			UnaryOperator<AdvancementHolder> advance = advancement -> {
				advancements.accept(advancement);
				return advancement;
			};
			// the root advancement
			var root = advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"root",
				"More Red",
				"There's a lot of redstone in your pockets",
				(title, description) -> AdvancementBuilder.root()
					.display(DisplayBuilder.of(MoreRed.RED_ALLOY_INGOT_ITEM.get(), title, description, AdvancementType.TASK)
						.background(Identifier.withDefaultNamespace("block/polished_blackstone"))
						.visibility(true, false, true))
					.tagStackCriterion(Tags.Items.DUSTS_REDSTONE, 64)));
						
			// transportation advancements
			advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_tube",
				"Square Hole",
				"Craft a Tube from Copper and Glass",
				(title, description) -> AdvancementBuilder.parented(root.id())
					.display(MoreRed.TUBE_BLOCK.get(), title, description, AdvancementType.TASK)
					.itemTagCriterion(MoreRed.Tags.Items.TUBES)));
			// TODO use pliers on tubes
			// redstone advancements
			advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_soldering_table",
				"Red Hot",
				"Craft a Soldering Table from Blaze Rods, Red Nether Bricks, and Stone Plates",
				(title,description) -> AdvancementBuilder.parented(root.id())
					.display(
						MoreRed.SOLDERING_TABLE_BLOCK.get(),
						title,
						description,
						AdvancementType.TASK)
					.itemCriterion(MoreRed.SOLDERING_TABLE_BLOCK.get())));
			var redstoneRevolution = advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_red_alloy_wire",
				"Redstone Revolution",
				"Craft copper and redstone into Red Alloy Ingots and then craft those into Red Alloy Wires",
				(title,description) -> AdvancementBuilder.parented(root.id())
					.display(MoreRed.RED_ALLOY_WIRE_BLOCK.get(), title, description, AdvancementType.TASK)
					.itemCriterion(MoreRed.RED_ALLOY_WIRE_BLOCK.get())));
			var tasteTheRainbow = advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_colored_cable",
				"Taste the Rainbow",
				"Craft a Colored Cable from Wool and Red Alloy Wires",
				(title, description) -> AdvancementBuilder.parented(redstoneRevolution.id())
					.display(MoreRed.COLORED_CABLE_BLOCKS.get(DyeColor.ORANGE).get(), title, description, AdvancementType.TASK)
					.itemTagCriterion(MoreRed.Tags.Items.COLORED_CABLES)));
			advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_bundled_cable",
				"Humble Bundle",
				"Craft a Bundled Cable from Colored Cables",
				(title, description) -> AdvancementBuilder.parented(tasteTheRainbow.id())
					.display(MoreRed.BUNDLED_CABLE_BLOCK.get(), title, description, AdvancementType.TASK)
					.itemCriterion(MoreRed.BUNDLED_CABLE_BLOCK.get())));
			// TODO use spool on junction
			// mechanical advancmennts
			var windyThing = advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_windcatcher",
				"The Windy Thing",
				"Craft a Windcatcher from an Axle, Sticks, and Wool",
				(title, description) -> AdvancementBuilder.parented(root.id())
					.display(MoreRed.WINDCATCHER_BLOCKS.get("oak").get(), title, description, AdvancementType.TASK)
					.itemTagCriterion(MoreRed.Tags.Items.WINDCATCHERS)));
			advance.apply(advancement(
				lang,
				MoreRed.MODID,
				"craft_mechanism",
				"Power User",
				"Craft an Alternator, Extractor, or Stonemill",
				(title, description) -> AdvancementBuilder.parented(windyThing.id())
					.display(MoreRed.EXTRACTOR_BLOCK.get(), title, description, AdvancementType.TASK)
					.itemsCriterion("items", MoreRed.EXTRACTOR_BLOCK.get(), MoreRed.ALTERNATOR_BLOCK.get(), MoreRed.STONEMILL_BLOCK.get())
					.requireAny()));
		})));
	}
	
	public static AdvancementHolder advancement(LanguageProvider lang, String tab, String path, String title, String description, BiFunction<String,String,AdvancementBuilder> advancement)
	{
		String namespace = MoreRed.MODID;
		String key = String.format("advancements.%s.%s.%s", namespace, tab, path);
		String titleKey = key + ".title";
		String descriptionKey = key + ".description";
		Identifier id = Identifier.fromNamespaceAndPath(namespace, tab + "/" + path);
		lang.add(titleKey, title);
		lang.add(descriptionKey, description);
		return advancement.apply(titleKey, descriptionKey).build(id);
	}
	
	// vanilla's builder does things we don't like
	// like deprecating the id-based advancement parent for removal, and requiring telemetry
	private static class AdvancementBuilder
	{
		AdvancementRequirements.Strategy strategy = AdvancementRequirements.Strategy.AND;
		Optional<Identifier> parent = Optional.empty();
		Optional<DisplayInfo> display = Optional.empty();
		Map<String, Criterion<?>> criteria = new HashMap<>();
		
		static AdvancementBuilder root()
		{
			return new AdvancementBuilder();
		}
		
		static AdvancementBuilder parented(Identifier parent)
		{
			AdvancementBuilder builder = new AdvancementBuilder();
			builder.parent = Optional.of(parent);
			return builder;
		}
		
		AdvancementBuilder display(ItemLike item, String titleKey, String descriptionKey, AdvancementType type)
		{
			this.display = Optional.of(new DisplayInfo(
				new ItemStack(item),
				Component.translatable(titleKey),
				Component.translatable(descriptionKey),
				Optional.empty(),
				type,
				true,
				true,
				false));
			return this;
		}
		
		AdvancementBuilder display(DisplayBuilder builder)
		{
			this.display = Optional.of(builder.build());
			return this;
		}
		
		AdvancementBuilder itemCriterion(ItemLike item)
		{
			this.criteria.put(
				BuiltInRegistries.ITEM.getKey(item.asItem()).toString(),
				InventoryChangeTrigger.TriggerInstance.hasItems(item));
			return this;
		}
		
		@SuppressWarnings("deprecation")
		AdvancementBuilder itemTagCriterion(TagKey<Item> tag)
		{
			this.criteria.put(
				tag.location().toString(),
				InventoryChangeTrigger.TriggerInstance.hasItems(new ItemPredicate(
					Optional.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag)),
					MinMaxBounds.Ints.ANY,
					DataComponentMatchers.ANY)));
			return this;
		}
		
		AdvancementBuilder itemsCriterion(String name, ItemLike... items)
		{
			this.criteria.put(
				name,
				InventoryChangeTrigger.TriggerInstance.hasItems(new ItemPredicate(
					Optional.of(HolderSet.direct(Arrays.stream(items)
						.<Holder<Item>>map(itemLike -> BuiltInRegistries.ITEM.wrapAsHolder(itemLike.asItem()))
						.toList())),
					MinMaxBounds.Ints.ANY,
					DataComponentMatchers.ANY)));
			return this;
		}
		
		@SuppressWarnings("deprecation")
		AdvancementBuilder tagStackCriterion(TagKey<Item> tag, int minCount)
		{
			this.criteria.put(
				tag.location().toString(),
				InventoryChangeTrigger.TriggerInstance.hasItems(new ItemPredicate(Optional.of(
					HolderSet.emptyNamed(BuiltInRegistries.ITEM, Tags.Items.DUSTS_REDSTONE)),
					MinMaxBounds.Ints.atLeast(64),
					DataComponentMatchers.ANY)));
			return this;
		}
		
		AdvancementBuilder requireAny()
		{
			this.strategy = AdvancementRequirements.Strategy.OR;
			return this;
		}
		
		AdvancementHolder build(Identifier id)
		{
			return new AdvancementHolder(id, new Advancement(
				this.parent,
				this.display,
				AdvancementRewards.EMPTY,
				this.criteria,
				this.strategy.create(this.criteria.keySet()),
				false));
		}
	}
	
	static class DisplayBuilder
	{
		private final ItemStack itemStack;
		private final String titleKey;
		private final String descriptionKey;
		private final AdvancementType type;
		private Optional<ResourceTexture> background = Optional.empty();
		private boolean showToast = true;
		private boolean announceToChat = true;
		private boolean hidden = false; 
		
		private DisplayBuilder(ItemStack itemStack, String titleKey, String descriptionKey, AdvancementType type)
		{
			this.itemStack = itemStack;
			this.titleKey = titleKey;
			this.descriptionKey = descriptionKey;
			this.type = type;
		}
		
		public static DisplayBuilder of(ItemLike itemLike, String titleKey, String descriptionKey, AdvancementType type)
		{
			return new DisplayBuilder(new ItemStack(itemLike), titleKey, descriptionKey, type);
		}
		
		public DisplayBuilder background(Identifier id)
		{
			this.background = Optional.of(new ResourceTexture(id));
			return this;
		}
		
		public DisplayBuilder visibility(boolean showToast, boolean announceToChat, boolean hidden)
		{
			this.showToast = showToast;
			this.announceToChat = announceToChat;
			this.hidden = hidden;
			return this;
		}
		
		public DisplayInfo build()
		{
			return new DisplayInfo(
				this.itemStack,
				Component.translatable(this.titleKey),
				Component.translatable(this.descriptionKey),
				this.background,
				this.type,
				this.showToast,
				this.announceToChat,
				this.hidden);
		}
	}
}
