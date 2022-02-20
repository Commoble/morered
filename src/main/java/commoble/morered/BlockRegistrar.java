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
import net.minecraft.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unchecked")
public class BlockRegistrar {
    // logic function blocks are registered elsewhere, see LogicGateType
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MoreRed.MODID);

    public static final RegistryObject<GatecraftingPlinthBlock> GATECRAFTING_PLINTH =
            BLOCKS.register(ObjectNames.GATECRAFTING_PLINTH,
                    () -> new GatecraftingPlinthBlock(Block.Properties.of(Material.STONE).strength(3.5F).noOcclusion()));

    // TODO fixme add pickaxe as tool
    public static final RegistryObject<PlateBlock> STONE_PLATE = BLOCKS.register(ObjectNames.STONE_PLATE,
            () -> new PlateBlock(Block.Properties.of(PlateBlockStateProperties.PLATE_MATERIAL)
                    .requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.WOOD)));

    public static final RegistryObject<LatchBlock> LATCH = BLOCKS.register(ObjectNames.LATCH,
            () -> new LatchBlock(Block.Properties.of(PlateBlockStateProperties.PLATE_MATERIAL).strength(0).sound(SoundType.WOOD)));

    public static final RegistryObject<WirePostBlock> REDWIRE_POST = BLOCKS.register(ObjectNames.REDWIRE_POST,
            () -> new WirePostBlock(Block.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F, 5F),
                    WirePostBlock::getRedstoneConnectionDirections));

    public static final RegistryObject<WirePostPlateBlock> REDWIRE_POST_PLATE =
            BLOCKS.register(ObjectNames.REDWIRE_POST_PLATE,
                    () -> new WirePostPlateBlock(Block.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F,
                            5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));

    public static final RegistryObject<WirePostPlateBlock> REDWIRE_POST_RELAY_PLATE =
            BLOCKS.register(ObjectNames.REDWIRE_POST_RELAY_PLATE,
                    () -> new WirePostPlateBlock(Block.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F,
                            5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));

    public static final RegistryObject<HexidecrubrometerBlock> HEXIDECRUBROMETER =
            BLOCKS.register(ObjectNames.HEXIDECRUBROMETER,
                    () -> new HexidecrubrometerBlock(Block.Properties.of(Material.STONE, MaterialColor.COLOR_RED).strength(2F
                            , 5F)));

    public static final RegistryObject<RedAlloyWireBlock> RED_ALLOY_WIRE = BLOCKS.register(ObjectNames.RED_ALLOY_WIRE,
            () -> new RedAlloyWireBlock(Block.Properties.of(Material.DECORATION, MaterialColor.COLOR_RED).noCollission().instabreak()));

    public static final RegistryObject<ColoredCableBlock>[] NETWORK_CABLES =
            Util.make((RegistryObject<ColoredCableBlock>[]) new RegistryObject[16], array ->
                    Arrays.setAll(array, i -> BLOCKS.register(ObjectNames.NETWORK_CABLES[i],
                            () -> new ColoredCableBlock(Block.Properties.of(Material.DECORATION,
                                    DyeColor.values()[i].getMaterialColor()).noCollission().instabreak(),
                                    DyeColor.values()[i]))));

    public static final RegistryObject<BundledCableBlock> BUNDLED_NETWORK_CABLE =
            BLOCKS.register(ObjectNames.BUNDLED_NETWORK_CABLE,
                    () -> new BundledCableBlock(Block.Properties.of(Material.DECORATION, MaterialColor.COLOR_BLUE).noCollission().instabreak()));

    public static final RegistryObject<BundledCablePostBlock> BUNDLED_CABLE_POST =
            BLOCKS.register(ObjectNames.BUNDLED_CABLE_POST,
                    () -> new BundledCablePostBlock(Block.Properties.of(Material.STONE, MaterialColor.COLOR_BLUE).strength(2F
                            , 5F)));

    public static final RegistryObject<BundledCableRelayPlateBlock> BUNDLED_CABLE_RELAY_PLATE =
            BLOCKS.register(ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
            () -> new BundledCableRelayPlateBlock(Block.Properties.of(Material.STONE, MaterialColor.COLOR_BLUE).strength(2F, 5F)));
}
