package commoble.morered;

import java.util.Arrays;

import commoble.morered.gatecrafting_plinth.GatecraftingPlinthBlock;
import commoble.morered.plate_blocks.LatchBlock;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.wire_post.BundledCablePostBlock;
import commoble.morered.wire_post.BundledCableRelayPlateBlock;
import commoble.morered.wire_post.WirePostBlock;
import commoble.morered.wire_post.WirePostPlateBlock;
import commoble.morered.wires.BundledCableBlock;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.RedAlloyWireBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.DyeColor;
import net.minecraft.util.Util;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unchecked")
public class BlockRegistrar
{
	// logic function blocks are registered elsewhere, see LogicGateType
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MoreRed.MODID);
	
	public static final RegistryObject<GatecraftingPlinthBlock> GATECRAFTING_PLINTH = BLOCKS.register(ObjectNames.GATECRAFTING_PLINTH,
		() -> new GatecraftingPlinthBlock(AbstractBlock.Properties.of(Material.STONE).strength(3.5F).noOcclusion()));

	public static final RegistryObject<PlateBlock> STONE_PLATE = BLOCKS.register(ObjectNames.STONE_PLATE,
		() -> new PlateBlock(AbstractBlock.Properties.of(PlateBlockStateProperties.PLATE_MATERIAL).harvestTool(ToolType.PICKAXE).requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.WOOD)));

	public static final RegistryObject<LatchBlock> LATCH = BLOCKS.register(ObjectNames.LATCH,
		() -> new LatchBlock(AbstractBlock.Properties.of(PlateBlockStateProperties.PLATE_MATERIAL).strength(0).sound(SoundType.WOOD)));

	public static final RegistryObject<WirePostBlock> REDWIRE_POST = BLOCKS.register(ObjectNames.REDWIRE_POST,
		() -> new WirePostBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F, 5F), WirePostBlock::getRedstoneConnectionDirections));
	
	public static final RegistryObject<WirePostPlateBlock> REDWIRE_POST_PLATE = BLOCKS.register(ObjectNames.REDWIRE_POST_PLATE,
		() -> new WirePostPlateBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));
	
	public static final RegistryObject<WirePostPlateBlock> REDWIRE_POST_RELAY_PLATE = BLOCKS.register(ObjectNames.REDWIRE_POST_RELAY_PLATE,
		() -> new WirePostPlateBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));
	
	public static final RegistryObject<HexidecrubrometerBlock> HEXIDECRUBROMETER = BLOCKS.register(ObjectNames.HEXIDECRUBROMETER,
		() -> new HexidecrubrometerBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F, 5F)));

	public static final RegistryObject<RedAlloyWireBlock> RED_ALLOY_WIRE = BLOCKS.register(ObjectNames.RED_ALLOY_WIRE, 
		() -> new RedAlloyWireBlock(AbstractBlock.Properties.of(Material.DECORATION, MaterialColor.COLOR_RED).noCollission().instabreak()));

	public static final RegistryObject<ColoredCableBlock>[] NETWORK_CABLES = Util.make((RegistryObject<ColoredCableBlock>[])new RegistryObject[16], array ->
		Arrays.setAll(array, i -> BLOCKS.register(ObjectNames.NETWORK_CABLES[i],
			() -> new ColoredCableBlock(AbstractBlock.Properties.of(Material.DECORATION, DyeColor.values()[i].getMaterialColor()).noCollission().instabreak(), DyeColor.values()[i]))));
	
	public static final RegistryObject<BundledCableBlock> BUNDLED_NETWORK_CABLE = BLOCKS.register(ObjectNames.BUNDLED_NETWORK_CABLE,
		() -> new BundledCableBlock(AbstractBlock.Properties.of(Material.DECORATION, MaterialColor.COLOR_BLUE).noCollission().instabreak()));
	
	public static final RegistryObject<BundledCablePostBlock> BUNDLED_CABLE_POST = BLOCKS.register(ObjectNames.BUNDLED_CABLE_POST,
		() -> new BundledCablePostBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_BLUE).strength(2F, 5F)));
	
	public static final RegistryObject<BundledCableRelayPlateBlock> BUNDLED_CABLE_RELAY_PLATE = BLOCKS.register(ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
		() -> new BundledCableRelayPlateBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_BLUE).strength(2F, 5F)));
}
