package commoble.morered;

import commoble.morered.gatecrafting_plinth.GatecraftingPlinthBlock;
import commoble.morered.plate_blocks.LatchBlock;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.wire_post.WirePostBlock;
import commoble.morered.wire_post.WirePostPlateBlock;
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
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MoreRed.MODID);
	
	public static final RegistryObject<GatecraftingPlinthBlock> GATECRAFTING_PLINTH = BLOCKS.register(ObjectNames.GATECRAFTING_PLINTH,
		() -> new GatecraftingPlinthBlock(Block.Properties.create(Material.ROCK).hardnessAndResistance(3.5F).notSolid()));

	public static final RegistryObject<PlateBlock> STONE_PLATE = BLOCKS.register(ObjectNames.STONE_PLATE,
		() -> new PlateBlock(Block.Properties.create(PlateBlockStateProperties.PLATE_MATERIAL).hardnessAndResistance(1.5F).sound(SoundType.WOOD)));

	public static final RegistryObject<LatchBlock> LATCH = BLOCKS.register(ObjectNames.LATCH,
		() -> new LatchBlock(Block.Properties.create(PlateBlockStateProperties.PLATE_MATERIAL).hardnessAndResistance(0).sound(SoundType.WOOD)));

	public static final RegistryObject<WirePostBlock> REDWIRE_POST = BLOCKS.register(ObjectNames.REDWIRE_POST,
		() -> new WirePostBlock(Block.Properties.create(Material.ROCK, MaterialColor.RED).hardnessAndResistance(2F, 5F), WirePostBlock::getRedstoneConnectionDirections));
	
	public static final RegistryObject<WirePostPlateBlock> REDWIRE_POST_PLATE = BLOCKS.register(ObjectNames.REDWIRE_POST_PLATE,
		() -> new WirePostPlateBlock(Block.Properties.create(Material.ROCK, MaterialColor.RED).hardnessAndResistance(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));
	
	public static final RegistryObject<WirePostPlateBlock> REDWIRE_POST_RELAY_PLATE = BLOCKS.register(ObjectNames.REDWIRE_POST_RELAY_PLATE,
		() -> new WirePostPlateBlock(Block.Properties.create(Material.ROCK, MaterialColor.RED).hardnessAndResistance(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));
	
	public static final RegistryObject<HexidecrubrometerBlock> HEXIDECRUBROMETER = BLOCKS.register(ObjectNames.HEXIDECRUBROMETER,
		() -> new HexidecrubrometerBlock(Block.Properties.create(Material.ROCK, MaterialColor.RED).hardnessAndResistance(2F, 5F)));
}
