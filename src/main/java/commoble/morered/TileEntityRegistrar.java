package commoble.morered;

import java.util.Arrays;

import commoble.morered.wire_post.BundledCablePostTileEntity;
import commoble.morered.wire_post.BundledCableRelayPlateTileEntity;
import commoble.morered.wire_post.WirePostTileEntity;
import commoble.morered.wires.BundledCableTileEntity;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.ColoredCableTileEntity;
import commoble.morered.wires.WireTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class TileEntityRegistrar
{
	public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MoreRed.MODID);
	
	public static final RegistryObject<TileEntityType<WirePostTileEntity>> REDWIRE_POST = TILES.register(ObjectNames.REDWIRE_POST,
		() -> TileEntityType.Builder.create(WirePostTileEntity::new,
			BlockRegistrar.REDWIRE_POST.get(),
			BlockRegistrar.REDWIRE_POST_PLATE.get(),
			BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<WireTileEntity>> WIRE = TILES.register(ObjectNames.WIRE,
		() -> TileEntityType.Builder.create(WireTileEntity::new,
			BlockRegistrar.RED_ALLOY_WIRE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<ColoredCableTileEntity>> COLORED_NETWORK_CABLE = TILES.register(ObjectNames.COLORED_NETWORK_CABLE,
		() -> TileEntityType.Builder.create(ColoredCableTileEntity::new,
			Arrays.stream(BlockRegistrar.NETWORK_CABLES)
				.map(RegistryObject::get)
				.toArray(ColoredCableBlock[]::new))
		.build(null));
	
	public static final RegistryObject<TileEntityType<BundledCableTileEntity>> BUNDLED_NETWORK_CABLE = TILES.register(ObjectNames.BUNDLED_NETWORK_CABLE,
		() -> TileEntityType.Builder.create(BundledCableTileEntity::new,
			BlockRegistrar.BUNDLED_NETWORK_CABLE.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<BundledCablePostTileEntity>> BUNDLED_CABLE_POST = TILES.register(ObjectNames.BUNDLED_CABLE_POST,
		() -> TileEntityType.Builder.create(BundledCablePostTileEntity::new,
			BlockRegistrar.BUNDLED_CABLE_POST.get())
		.build(null));
	
	public static final RegistryObject<TileEntityType<BundledCableRelayPlateTileEntity>> BUNDLED_CABLE_RELAY_PLATE = TILES.register(ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
		() -> TileEntityType.Builder.create(BundledCableRelayPlateTileEntity::new,
			BlockRegistrar.BUNDLED_CABLE_RELAY_PLATE.get())
		.build(null));
}
