package commoble.morered;

import java.util.Arrays;

import commoble.morered.bitwise_logic.ChanneledPowerStorageTileEntity;
import commoble.morered.plate_blocks.LogicGateType;
import commoble.morered.wire_post.BundledCablePostTileEntity;
import commoble.morered.wire_post.BundledCableRelayPlateTileEntity;
import commoble.morered.wire_post.WirePostTileEntity;
import commoble.morered.wires.BundledCableTileEntity;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.ColoredCableTileEntity;
import commoble.morered.wires.WireTileEntity;
import net.minecraft.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TileEntityRegistrar {
    public static final DeferredRegister<BlockEntityType<?>> TILES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MoreRed.MODID);

    public static final RegistryObject<BlockEntityType<WirePostTileEntity>> REDWIRE_POST =
            TILES.register(ObjectNames.REDWIRE_POST,
                    () -> BlockEntityType.Builder.of(WirePostTileEntity::new,
                                    BlockRegistrar.REDWIRE_POST.get(),
                                    BlockRegistrar.REDWIRE_POST_PLATE.get(),
                                    BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<WireTileEntity>> WIRE = TILES.register(ObjectNames.WIRE,
            () -> BlockEntityType.Builder.of(WireTileEntity::new, BlockRegistrar.RED_ALLOY_WIRE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<ColoredCableTileEntity>> COLORED_NETWORK_CABLE =
            TILES.register(ObjectNames.COLORED_NETWORK_CABLE,
                    () -> BlockEntityType.Builder.of(ColoredCableTileEntity::new,
                                    Arrays.stream(BlockRegistrar.NETWORK_CABLES)
                                            .map(RegistryObject::get)
                                            .toArray(ColoredCableBlock[]::new))
                            .build(null));

    public static final RegistryObject<BlockEntityType<BundledCableTileEntity>> BUNDLED_NETWORK_CABLE =
            TILES.register(ObjectNames.BUNDLED_NETWORK_CABLE,
                    () -> BlockEntityType.Builder.of(BundledCableTileEntity::new, BlockRegistrar.BUNDLED_NETWORK_CABLE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<BundledCablePostTileEntity>> BUNDLED_CABLE_POST =
            TILES.register(ObjectNames.BUNDLED_CABLE_POST,
                    () -> BlockEntityType.Builder.of(BundledCablePostTileEntity::new,
                                    BlockRegistrar.BUNDLED_CABLE_POST.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<BundledCableRelayPlateTileEntity>> BUNDLED_CABLE_RELAY_PLATE =
            TILES.register(ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
                    () -> BlockEntityType.Builder.of(BundledCableRelayPlateTileEntity::new,
                                    BlockRegistrar.BUNDLED_CABLE_RELAY_PLATE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<ChanneledPowerStorageTileEntity>> BITWISE_LOGIC_PLATE =
            TILES.register(ObjectNames.BITWISE_LOGIC_PLATE,
            () -> BlockEntityType.Builder.of(ChanneledPowerStorageTileEntity::new,
                            Util.make(() ->
                            {    // valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
                                return LogicGateType.BITWISE_TYPES.values().stream()
                                        .map(type -> type.blockGetter.get())
                                        .toArray(Block[]::new);
                            }))
                    .build(null));

}
