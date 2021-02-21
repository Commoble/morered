package commoble.morered;

import java.util.Arrays;

import commoble.morered.wire_post.WireSpoolItem;
import commoble.morered.wires.WireBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;

public class ItemRegistrar
{
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MoreRed.MODID);
	
	// using an objectholder here for the creative tab since the function gates are procedurally generated
	@ObjectHolder("morered:nor_gate")
	public static final BlockItem NOR_GATE = null;
	
	public static final ItemGroup CREATIVE_TAB = new ItemGroup(MoreRed.MODID)
	{
		@Override
		public ItemStack createIcon()
		{
			return new ItemStack(ItemRegistrar.NOR_GATE);
		}
	};
	
	public static final RegistryObject<WireSpoolItem> REDWIRE_SPOOL = ITEMS.register(ObjectNames.REDWIRE_SPOOL, () -> new WireSpoolItem(new Item.Properties().group(CREATIVE_TAB).maxDamage(64)));
	public static final RegistryObject<Item> RED_ALLOY_INGOT = ITEMS.register(ObjectNames.RED_ALLOY_INGOT, () -> new Item(new Item.Properties().group(CREATIVE_TAB)));

	public static final RegistryObject<BlockItem> GATECRAFTING_PLINTH = registerBlockItem(ObjectNames.GATECRAFTING_PLINTH, BlockRegistrar.GATECRAFTING_PLINTH);
	public static final RegistryObject<BlockItem> REDWIRE_POST = registerBlockItem(ObjectNames.REDWIRE_POST, BlockRegistrar.REDWIRE_POST);
	public static final RegistryObject<BlockItem> REDWIRE_POST_PLATE = registerBlockItem(ObjectNames.REDWIRE_POST_PLATE, BlockRegistrar.REDWIRE_POST_PLATE);
	public static final RegistryObject<BlockItem> REDWIRE_POST_RELAY_PLATE = registerBlockItem(ObjectNames.REDWIRE_POST_RELAY_PLATE, BlockRegistrar.REDWIRE_POST_RELAY_PLATE);
	public static final RegistryObject<BlockItem> HEXIDECRUBROMETER = registerBlockItem(ObjectNames.HEXIDECRUBROMETER, BlockRegistrar.HEXIDECRUBROMETER);
	
	public static final RegistryObject<BlockItem> STONE_PLATE = registerBlockItem(ObjectNames.STONE_PLATE, BlockRegistrar.STONE_PLATE);
	public static final RegistryObject<BlockItem> LATCH = registerBlockItem(ObjectNames.LATCH, BlockRegistrar.LATCH);
	
	public static final RegistryObject<BlockItem> RED_ALLOY_WIRE = ITEMS.register(ObjectNames.RED_ALLOY_WIRE,
		() -> new WireBlockItem(BlockRegistrar.RED_ALLOY_WIRE.get(), new Item.Properties().group(CREATIVE_TAB)));
	
	@SuppressWarnings("unchecked")
	public static final RegistryObject<WireBlockItem>[] NETWORK_CABLES = Util.make((RegistryObject<WireBlockItem>[])new RegistryObject[16], array ->
		Arrays.setAll(array, i -> ITEMS.register(ObjectNames.NETWORK_CABLES[i],
			() -> new WireBlockItem(BlockRegistrar.NETWORK_CABLES[i].get(), new Item.Properties().group(CREATIVE_TAB)))));
	
	public static final RegistryObject<WireBlockItem> BUNDLED_NETWORK_CABLE = ITEMS.register(ObjectNames.BUNDLED_NETWORK_CABLE,
		() -> new WireBlockItem(BlockRegistrar.BUNDLED_NETWORK_CABLE.get(), new Item.Properties().group(CREATIVE_TAB)));

	
	public static final RegistryObject<BlockItem> registerBlockItem(String name, RegistryObject<? extends Block> block)
	{
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().group(CREATIVE_TAB)));
	}
}
