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
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Util;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class TileEntityRegistrar
{
	public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MoreRed.MODID);
	
	public static final RegistryObject<TileEntityType<WirePostTileEntity>> REDWIRE_POST = TILES.register(ObjectNames.REDWIRE_POST,
		() -> TileEntityType.Builder.of(WirePostTileEntity::new,
			BlockRegistrar.REDWIRE_POST.get(),
			BlockRegistrar.REDWIRE_POST_PLATE.get(),
			BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<WireTileEntity>> WIRE = TILES.register(ObjectNames.WIRE,
		() -> TileEntityType.Builder.of(WireTileEntity::new,
			BlockRegistrar.RED_ALLOY_WIRE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<ColoredCableTileEntity>> COLORED_NETWORK_CABLE = TILES.register(ObjectNames.COLORED_NETWORK_CABLE,
		() -> TileEntityType.Builder.of(ColoredCableTileEntity::new,
			Arrays.stream(BlockRegistrar.NETWORK_CABLES)
				.map(RegistryObject::get)
				.toArray(ColoredCableBlock[]::new))
		.build(null));
	
	public static final RegistryObject<TileEntityType<BundledCableTileEntity>> BUNDLED_NETWORK_CABLE = TILES.register(ObjectNames.BUNDLED_NETWORK_CABLE,
		() -> TileEntityType.Builder.of(BundledCableTileEntity::new,
			BlockRegistrar.BUNDLED_NETWORK_CABLE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<BundledCablePostTileEntity>> BUNDLED_CABLE_POST = TILES.register(ObjectNames.BUNDLED_CABLE_POST,
		() -> TileEntityType.Builder.of(BundledCablePostTileEntity::new,
			BlockRegistrar.BUNDLED_CABLE_POST.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<BundledCableRelayPlateTileEntity>> BUNDLED_CABLE_RELAY_PLATE = TILES.register(ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
		() -> TileEntityType.Builder.of(BundledCableRelayPlateTileEntity::new,
			BlockRegistrar.BUNDLED_CABLE_RELAY_PLATE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<ChanneledPowerStorageTileEntity>> BITWISE_LOGIC_PLATE = TILES.register(ObjectNames.BITWISE_LOGIC_PLATE,
		() -> TileEntityType.Builder.of(ChanneledPowerStorageTileEntity::new,
			Util.make(() ->
			{	// valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
				return LogicGateType.BITWISE_TYPES.values().stream()
					.map(type -> type.blockGetter.get())
					.toArray(Block[]::new);
			}))
		.build(null));
	
}
