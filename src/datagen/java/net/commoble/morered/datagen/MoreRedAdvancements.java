package net.commoble.morered.datagen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import net.commoble.morered.MoreRed;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

// advancements are totally unrelated to everything else
// so I don't mind doing them in a separate class
public class MoreRedAdvancements
{
	public static void genAdvancements(DataGenContext context)
	{
		// the root advancement
		Identifier root = advancement(
			context,
			MoreRed.MODID,
			"root",
			"More Red",
			"There's a lot of redstone in your pockets",
			(title, description) -> AdvancementBuilder.root()
				.display(DisplayBuilder.of(MoreRed.RED_ALLOY_INGOT_ITEM.get(), title, description, AdvancementType.TASK)
					.background(Identifier.withDefaultNamespace("block/polished_blackstone"))
					.visibility(true, false, true))
				.tagStackCriterion(Tags.Items.DUSTS_REDSTONE, 64));
						
		// transportation advancements
		advancement(
			context,
			MoreRed.MODID,
			"craft_tube",
			"Square Hole",
			"Craft a Tube from Copper and Glass",
			(title, description) -> AdvancementBuilder.parented(root)
				.display(MoreRed.TUBE_BLOCK.get(), title, description, AdvancementType.TASK)
				.itemTagCriterion(MoreRed.Tags.Items.TUBES));
		// TODO use pliers on tubes
		// redstone advancements
		advancement(
			context,
			MoreRed.MODID,
			"craft_soldering_table",
			"Red Hot",
			"Craft a Soldering Table from Blaze Rods, Red Nether Bricks, and Stone Plates",
			(title,description) -> AdvancementBuilder.parented(root)
				.display(
					MoreRed.SOLDERING_TABLE_BLOCK.get(),
					title,
					description,
					AdvancementType.TASK)
				.itemCriterion(MoreRed.SOLDERING_TABLE_BLOCK.get()));
		Identifier redstoneRevolution = advancement(
			context,
			MoreRed.MODID,
			"craft_red_alloy_wire",
			"Redstone Revolution",
			"Craft copper and redstone into Red Alloy Ingots and then craft those into Red Alloy Wires",
			(title,description) -> AdvancementBuilder.parented(root)
				.display(MoreRed.RED_ALLOY_WIRE_BLOCK.get(), title, description, AdvancementType.TASK)
				.itemCriterion(MoreRed.RED_ALLOY_WIRE_BLOCK.get()));
		Identifier tasteTheRainbow = advancement(
			context,
			MoreRed.MODID,
			"craft_colored_cable",
			"Taste the Rainbow",
			"Craft a Colored Cable from Wool and Red Alloy Wires",
			(title, description) -> AdvancementBuilder.parented(redstoneRevolution)
				.display(MoreRed.COLORED_CABLE_BLOCKS.get(DyeColor.ORANGE).get(), title, description, AdvancementType.TASK)
				.itemTagCriterion(MoreRed.Tags.Items.COLORED_CABLES));
		advancement(
			context,
			MoreRed.MODID,
			"craft_bundled_cable",
			"Humble Bundle",
			"Craft a Bundled Cable from Colored Cables",
			(title, description) -> AdvancementBuilder.parented(tasteTheRainbow)
				.display(MoreRed.BUNDLED_CABLE_BLOCK.get(), title, description, AdvancementType.TASK)
				.itemCriterion(MoreRed.BUNDLED_CABLE_BLOCK.get()));
		// TODO use spool on junction
		// mechanical advancmennts
		Identifier windyThing = advancement(
			context,
			MoreRed.MODID,
			"craft_windcatcher",
			"The Windy Thing",
			"Craft a Windcatcher from an Axle, Sticks, and Wool",
			(title, description) -> AdvancementBuilder.parented(root)
				.display(MoreRed.WINDCATCHER_BLOCKS.get("oak").get(), title, description, AdvancementType.TASK)
				.itemTagCriterion(MoreRed.Tags.Items.WINDCATCHERS));
		advancement(
			context,
			MoreRed.MODID,
			"craft_mechanism",
			"Power User",
			"Craft an Alternator, Extractor, or Stonemill",
			(title, description) -> AdvancementBuilder.parented(windyThing)
				.display(MoreRed.EXTRACTOR_BLOCK.get(), title, description, AdvancementType.TASK)
				.itemsCriterion("items", MoreRed.EXTRACTOR_BLOCK.get(), MoreRed.ALTERNATOR_BLOCK.get(), MoreRed.STONEMILL_BLOCK.get())
				.requireAny());
	}
	
	private static Identifier advancement(DataGenContext context, String tab, String path, String title, String description, BiFunction<String,String,AdvancementBuilder> advancement)
	{
		String namespace = MoreRed.MODID;
		String key = String.format("advancements.%s.%s.%s", namespace, tab, path);
		String titleKey = key + ".title";
		String descriptionKey = key + ".description";
		Identifier id = Identifier.fromNamespaceAndPath(namespace, tab + "/" + path);
		context.lang().add(langProvider -> langProvider.add(titleKey, title));
		context.lang().add(langProvider -> langProvider.add(descriptionKey, description));
		context.advancements().put(id, advancements -> advancement.apply(titleKey, descriptionKey).build(id));
		return id;
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
				new ItemStackTemplate(item.asItem()),
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
		
		Advancement build(Identifier id)
		{
			return new Advancement(
				this.parent,
				this.display,
				AdvancementRewards.EMPTY,
				this.criteria,
				this.strategy.create(this.criteria.keySet()),
				false);
		}
	}
	
	static class DisplayBuilder
	{
		private final ItemStackTemplate itemStack;
		private final String titleKey;
		private final String descriptionKey;
		private final AdvancementType type;
		private Optional<ResourceTexture> background = Optional.empty();
		private boolean showToast = true;
		private boolean announceToChat = true;
		private boolean hidden = false; 
		
		private DisplayBuilder(ItemStackTemplate itemStack, String titleKey, String descriptionKey, AdvancementType type)
		{
			this.itemStack = itemStack;
			this.titleKey = titleKey;
			this.descriptionKey = descriptionKey;
			this.type = type;
		}
		
		public static DisplayBuilder of(ItemLike itemLike, String titleKey, String descriptionKey, AdvancementType type)
		{
			return new DisplayBuilder(new ItemStackTemplate(itemLike.asItem()), titleKey, descriptionKey, type);
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
