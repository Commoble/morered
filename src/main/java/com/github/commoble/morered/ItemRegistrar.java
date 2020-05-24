package com.github.commoble.morered;

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
	
	public static final RegistryObject<Item> NOT_GATE =
		ITEMS.register(ObjectNames.NOT_GATE, () -> new BlockItem(BlockRegistrar.NOT_GATE.get(), new Item.Properties().group(CREATIVE_TAB)));

}
