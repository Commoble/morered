package commoble.morered;

import commoble.morered.wire_post.WirePostTileEntity;
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
}
