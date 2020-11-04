package commoble.morered.plate_blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.plate_blocks.LogicFunctionPlateBlock.LogicFunctionPlateBlockFactory;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.block.AbstractBlock;

public class LogicGateType
{
	public static final Map<ResourceLocation, LogicGateType> TYPES = new HashMap<>();
	
	public final Supplier<LogicFunctionPlateBlock> blockGetter;
	public final Supplier<BlockItem> itemGetter;
	
	public LogicGateType(Supplier<LogicFunctionPlateBlock> blockGetter, Supplier<BlockItem> itemGetter)
	{
		this.blockGetter = blockGetter;
		this.itemGetter = itemGetter;
	}
	
	public static void registerLogicGateType(String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, LogicFunctionPlateBlockFactory inputs)
	{
		registerLogicGateType(MoreRed.MODID, name, blocks, items, function, inputs);
	}
	
	/** Call from mod constructor **/
	public static void registerLogicGateType(String modid, String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, LogicFunctionPlateBlockFactory factory)
	{
		ResourceLocation id = new ResourceLocation(modid, name);
		Supplier<LogicFunctionPlateBlock> blockGetter = registerLogicGate(blocks, name, function, factory);
		Supplier<BlockItem> itemGetter = registerBlockItem(items, name, blockGetter);
		TYPES.put(id, new LogicGateType(blockGetter, itemGetter));
	}
	
	private static RegistryObject<LogicFunctionPlateBlock> registerLogicGate(DeferredRegister<Block> blocks, String name, LogicFunction function, LogicFunctionPlateBlockFactory factory)
	{
		return blocks.register(name, () -> factory.makeBlock(function,
			AbstractBlock.Properties.create(PlateBlockStateProperties.PLATE_MATERIAL).hardnessAndResistance(0F).sound(SoundType.WOOD)));
			
	}
	
	private static final RegistryObject<BlockItem> registerBlockItem(DeferredRegister<Item> items, String name, Supplier<? extends Block> blockGetter)
	{
		return items.register(name, () -> new BlockItem(blockGetter.get(), new Item.Properties().group(ItemRegistrar.CREATIVE_TAB)));
	}

	
	/** Called from MoreRed mod constructor **/
	public static void registerLogicGateTypes(DeferredRegister<Block> blocks, DeferredRegister<Item> items)
	{
		registerLogicGateType(ObjectNames.DIODE, blocks, items, LogicFunctions.INPUT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
		registerLogicGateType(ObjectNames.NOT_GATE, blocks, items, LogicFunctions.NOT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
		registerLogicGateType(ObjectNames.NOR_GATE, blocks, items, LogicFunctions.NOR, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(ObjectNames.NAND_GATE, blocks, items, LogicFunctions.NAND, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(ObjectNames.OR_GATE, blocks, items, LogicFunctions.OR, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(ObjectNames.AND_GATE, blocks, items, LogicFunctions.AND, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(ObjectNames.XOR_GATE, blocks, items, LogicFunctions.XOR_AC, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(ObjectNames.XNOR_GATE, blocks, items, LogicFunctions.XNOR_AC, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(ObjectNames.MULTIPLEXER, blocks, items, LogicFunctions.MULTIPLEX, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(ObjectNames.AND_2_GATE, blocks, items, LogicFunctions.AND_2, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(ObjectNames.NAND_2_GATE, blocks, items, LogicFunctions.NAND_2, LogicFunctionPlateBlock.T_INPUTS);
	}
}
