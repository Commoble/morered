package com.github.commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockRegistrar
{
	// logic function blocks are registered elsewhere, see LogicGateType
	public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MoreRed.MODID);
	
	public static final RegistryObject<PlateBlock> STONE_PLATE = BLOCKS.register(ObjectNames.STONE_PLATE,
		() -> new PlateBlock(Block.Properties.create(PlateBlockStateProperties.PLATE_MATERIAL).hardnessAndResistance(0.4F).sound(SoundType.WOOD)));
}
