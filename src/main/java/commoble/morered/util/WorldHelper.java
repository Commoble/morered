package commoble.morered.util;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WorldHelper {
    public static <T extends BlockEntity> Optional<T> getTileEntityAt(Class<T> clazz, LevelAccessor world,
                                                                      BlockPos pos) {
        return ClassHelper.as(world.getBlockEntity(pos), clazz);
    }
}
