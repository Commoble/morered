package commoble.morered.util;

import java.util.Optional;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

public class WorldHelper
{
	public static <T extends BlockEntity> Optional<T> getTileEntityAt(Class<T> clazz, LevelReader world, BlockPos pos)
	{
		return ClassHelper.as(world.getBlockEntity(pos), clazz);
	}
}
