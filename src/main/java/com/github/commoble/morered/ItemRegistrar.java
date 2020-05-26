package com.github.commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemRegistrar
{
	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MoreRed.MODID);
	
	public static final ItemGroup CREATIVE_TAB = new ItemGroup(MoreRed.MODID)
	{
		@Override
		public ItemStack createIcon()
		{
			return new ItemStack(Items.REPEATER);
		}
	};
	
	public static final RegistryObject<BlockItem> NOT_GATE = registerBlockItem(ObjectNames.NOT_GATE, BlockRegistrar.NOT_GATE);
	public static final RegistryObject<BlockItem> NOR_GATE = registerBlockItem(ObjectNames.NOR_GATE, BlockRegistrar.NOR_GATE);
	
	public static final RegistryObject<BlockItem> registerBlockItem(String name, RegistryObject<? extends Block> block)
	{
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().group(CREATIVE_TAB)));
	}
}
