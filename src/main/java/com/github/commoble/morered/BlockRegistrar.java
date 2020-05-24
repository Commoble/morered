package com.github.commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockRegistrar
{
	public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MoreRed.MODID);
	
	public static final RegistryObject<Block> NOT_GATE =
		BLOCKS.register(ObjectNames.NOT_GATE, () -> new NotGateBlock(Block.Properties.create(Material.MISCELLANEOUS).hardnessAndResistance(0).sound(SoundType.WOOD)));
}
