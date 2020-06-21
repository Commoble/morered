package com.github.commoble.morered;

import com.github.commoble.morered.gatecrafting_plinth.GatecraftingPlinthBlock;
import com.github.commoble.morered.plate_blocks.LatchBlock;
import com.github.commoble.morered.plate_blocks.PlateBlock;
import com.github.commoble.morered.plate_blocks.PlateBlockStateProperties;
import com.github.commoble.morered.wire_post.WirePostBlock;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockRegistrar
{
	// logic function blocks are registered elsewhere, see LogicGateType
	public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MoreRed.MODID);
	
	public static final RegistryObject<GatecraftingPlinthBlock> GATECRAFTING_PLINTH = BLOCKS.register(ObjectNames.GATECRAFTING_PLINTH,
		() -> new GatecraftingPlinthBlock(Block.Properties.create(Material.ROCK).hardnessAndResistance(3.5F).notSolid()));

	public static final RegistryObject<PlateBlock> STONE_PLATE = BLOCKS.register(ObjectNames.STONE_PLATE,
		() -> new PlateBlock(Block.Properties.create(PlateBlockStateProperties.PLATE_MATERIAL).hardnessAndResistance(1.5F).sound(SoundType.WOOD)));

	public static final RegistryObject<LatchBlock> LATCH = BLOCKS.register(ObjectNames.LATCH,
		() -> new LatchBlock(Block.Properties.create(PlateBlockStateProperties.PLATE_MATERIAL).hardnessAndResistance(0).sound(SoundType.WOOD)));

	public static final RegistryObject<WirePostBlock> REDWIRE_POST = BLOCKS.register(ObjectNames.REDWIRE_POST,
		() -> new WirePostBlock(Block.Properties.create(Material.ROCK, MaterialColor.RED).hardnessAndResistance(2F, 5F)));
	
}
