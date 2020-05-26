package com.github.commoble.morered;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class LogicGateType
{
	public static final Map<ResourceLocation, LogicGateType> TYPES = new HashMap<>();
	
	public final Supplier<LogicGatePlateBlock> blockGetter;
	public final Supplier<BlockItem> itemGetter;
	public final LogicFunction function;
	public final InputType inputs;
	
	public LogicGateType(Supplier<LogicGatePlateBlock> blockGetter, Supplier<BlockItem> itemGetter, LogicFunction function, InputType inputs)
	{
		this.blockGetter = blockGetter;
		this.itemGetter = itemGetter;
		this.function = function;
		this.inputs = inputs;
	}
	
	public static void registerLogicGateType(String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, InputType inputs)
	{
		registerLogicGateType(MoreRed.MODID, name, blocks, items, function, inputs);
	}
	
	/** Call from mod constructor **/
	public static void registerLogicGateType(String modid, String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, InputType inputs)
	{
		ResourceLocation id = new ResourceLocation(modid, name);
		Supplier<LogicGatePlateBlock> blockGetter = registerLogicGate(blocks, name, function, inputs);
		Supplier<BlockItem> itemGetter = registerBlockItem(items, name, blockGetter);
		TYPES.put(id, new LogicGateType(blockGetter, itemGetter, function, inputs));
	}
	
	private static RegistryObject<LogicGatePlateBlock> registerLogicGate(DeferredRegister<Block> blocks, String name, LogicFunction function, InputType inputs)
	{
		return blocks.register(name, () -> inputs.plateMaker.makeBlock(function,
			Block.Properties.create(Material.MISCELLANEOUS).hardnessAndResistance(0).sound(SoundType.WOOD)));
			
	}
	
	private static final RegistryObject<BlockItem> registerBlockItem(DeferredRegister<Item> items, String name, Supplier<? extends Block> blockGetter)
	{
		return items.register(name, () -> new BlockItem(blockGetter.get(), new Item.Properties().group(ItemRegistrar.CREATIVE_TAB)));
	}

	
	/** Called from MoreRed mod constructor **/
	public static void registerLogicGateTypes(DeferredRegister<Block> blocks, DeferredRegister<Item> items)
	{
		registerLogicGateType(ObjectNames.NOT_GATE, blocks, items, LogicFunctions.NOT_B, InputSide.LINEAR_INPUT);
		registerLogicGateType(ObjectNames.NOR_GATE, blocks, items, LogicFunctions.NOR, InputSide.THREE_INPUTS);
		registerLogicGateType(ObjectNames.NAND_GATE, blocks, items, LogicFunctions.NAND, InputSide.THREE_INPUTS);
		registerLogicGateType(ObjectNames.OR_GATE, blocks, items, LogicFunctions.OR, InputSide.THREE_INPUTS);
		registerLogicGateType(ObjectNames.AND_GATE, blocks, items, LogicFunctions.AND, InputSide.THREE_INPUTS);
		registerLogicGateType(ObjectNames.XOR_GATE, blocks, items, LogicFunctions.XOR_AC, InputSide.T_INPUTS);
		registerLogicGateType(ObjectNames.XNOR_GATE, blocks, items, LogicFunctions.XNOR_AC, InputSide.T_INPUTS);
		registerLogicGateType(ObjectNames.MULTIPLEXER, blocks, items, LogicFunctions.MULTIPLEX, InputSide.THREE_INPUTS);
	}
}
