package commoble.morered.plate_blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import commoble.morered.BlockAndBlockItem;
import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.bitwise_logic.BitwiseLogicPlateBlock;
import commoble.morered.bitwise_logic.SingleInputBitwiseLogicPlateBlock;
import commoble.morered.bitwise_logic.TwoInputBitwiseLogicPlateBlock;
import commoble.morered.plate_blocks.LogicFunctionPlateBlock.LogicFunctionPlateBlockFactory;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class LogicGateType extends BlockAndBlockItem<Block, BlockItem>
{
	public static final Map<ResourceLocation, LogicGateType> TYPES = new HashMap<>();
	public static final Map<ResourceLocation, BlockAndBlockItem<? extends BitwiseLogicPlateBlock, BlockItem>> BITWISE_TYPES = new HashMap<>();

	public LogicGateType(Supplier<? extends Block> blockGetter, Supplier<? extends BlockItem> itemGetter)
	{
		super(blockGetter, itemGetter);
	}
	
	public static void registerLogicGateType(String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, LogicFunctionPlateBlockFactory inputs)
	{
		registerLogicGateType(MoreRed.MODID, name, blocks, items, function, inputs);
	}
	
	// Call from mod constructor
	public static void registerLogicGateType(String modid, String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, LogicFunctionPlateBlockFactory factory)
	{
		ResourceLocation id = new ResourceLocation(modid, name);
		Supplier<? extends Block> blockGetter = registerLogicGate(blocks, name, function, factory);
		Supplier<BlockItem> itemGetter = registerBlockItem(items, name, blockGetter);
		TYPES.put(id, new LogicGateType(blockGetter, itemGetter));
	}
	
	private static RegistryObject<LogicFunctionPlateBlock> registerLogicGate(DeferredRegister<Block> blocks, String name, LogicFunction function, LogicFunctionPlateBlockFactory factory)
	{
		return blocks.register(name, () -> factory.makeBlock(function,
			AbstractBlock.Properties.of(PlateBlockStateProperties.PLATE_MATERIAL).strength(0F).sound(SoundType.WOOD)));
			
	}
	
	private static final RegistryObject<BlockItem> registerBlockItem(DeferredRegister<Item> items, String name, Supplier<? extends Block> blockGetter)
	{
		return items.register(name, () -> new BlockItem(blockGetter.get(), new Item.Properties().tab(ItemRegistrar.CREATIVE_TAB)));
	}
	
	public static <B extends BitwiseLogicPlateBlock> void registerBitwiseLogicGateType(String name, DeferredRegister<Block> blocks, DeferredRegister<Item> items, LogicFunction function, BiFunction<AbstractBlock.Properties, LogicFunction, B> blockFactory)
	{
		RegistryObject<B> blockGetter = blocks.register(name, () -> blockFactory.apply(
			AbstractBlock.Properties.of(PlateBlockStateProperties.PLATE_MATERIAL, MaterialColor.QUARTZ).strength(0F).sound(SoundType.WOOD), function));
		Supplier<BlockItem> itemGetter = registerBlockItem(items, name, blockGetter);
		BITWISE_TYPES.put(blockGetter.getId(), new BlockAndBlockItem<>(blockGetter, itemGetter));
	}

	
	// Called from MoreRed mod constructor
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
		
		// bitwise logic gates store state in a TE instead of block properties
		// they don't need to have their properties defined on construction but they do need to be registered to the TE type they use
		BiFunction<AbstractBlock.Properties, LogicFunction, SingleInputBitwiseLogicPlateBlock> singleInput = SingleInputBitwiseLogicPlateBlock::new;
		BiFunction<AbstractBlock.Properties, LogicFunction, TwoInputBitwiseLogicPlateBlock> twoInputs = TwoInputBitwiseLogicPlateBlock::new;
		registerBitwiseLogicGateType(ObjectNames.BITWISE_DIODE, blocks, items, LogicFunctions.INPUT_B, singleInput);
		registerBitwiseLogicGateType(ObjectNames.BITWISE_NOT_GATE, blocks, items, LogicFunctions.NOT_B, singleInput);
		registerBitwiseLogicGateType(ObjectNames.BITWISE_OR_GATE, blocks, items, LogicFunctions.OR, twoInputs);
		registerBitwiseLogicGateType(ObjectNames.BITWISE_AND_GATE, blocks, items, LogicFunctions.AND_2, twoInputs);
		registerBitwiseLogicGateType(ObjectNames.BITWISE_XOR_GATE, blocks, items, LogicFunctions.XOR_AC, twoInputs);
		registerBitwiseLogicGateType(ObjectNames.BITWISE_XNOR_GATE, blocks, items, LogicFunctions.XNOR_AC, twoInputs);
	}
}
