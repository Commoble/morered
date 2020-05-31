package com.github.commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;

public class ItemRegistrar
{
	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MoreRed.MODID);
	
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
	
	public static final RegistryObject<BlockItem> STONE_PLATE = registerBlockItem(ObjectNames.STONE_PLATE, BlockRegistrar.STONE_PLATE);
	
	public static final RegistryObject<BlockItem> registerBlockItem(String name, RegistryObject<? extends Block> block)
	{
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().group(CREATIVE_TAB)));
	}
}
