package commoble.morered.util;

import java.util.Optional;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class WorldHelper
{
	public static <T extends TileEntity> Optional<T> getTileEntityAt(Class<T> clazz, IWorldReader world, BlockPos pos)
	{
		return ClassHelper.as(world.getTileEntity(pos), clazz);
	}
}
