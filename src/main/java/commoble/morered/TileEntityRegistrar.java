package commoble.morered;

import commoble.morered.wire_post.WirePostTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class TileEntityRegistrar
{
	public static final DeferredRegister<TileEntityType<?>> TILES = new DeferredRegister<>(ForgeRegistries.TILE_ENTITIES, MoreRed.MODID);
	
	public static final RegistryObject<TileEntityType<WirePostTileEntity>> REDWIRE_POST = TILES.register(ObjectNames.REDWIRE_POST,
		() -> TileEntityType.Builder.create(WirePostTileEntity::new, BlockRegistrar.REDWIRE_POST.get()).build(null));
}
