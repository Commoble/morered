package commoble.morered.wires;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.shapes.*;

public class VoxelCache extends SavedData {
    public static final String ID = "morered:voxelcache";
    private static VoxelCache clientCache = null;

    protected final Level world;
    public final LoadingCache<BlockPos, VoxelShape> shapesByPos;

    public VoxelCache(@Nonnull Level world) {
        super();
        this.world = world;
        this.shapesByPos = CacheBuilder.newBuilder()
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .build(new VoxelLoader());
    }

    public static VoxelCache get(@Nonnull Level world) {
        if (world instanceof ServerLevel) {
            return ((ServerLevel) world).getDataStorage().computeIfAbsent((c) -> new VoxelCache(world),
                    () -> new VoxelCache(world), ID);
        } else {
            if (clientCache == null || clientCache.world != world) {
                clientCache = new VoxelCache(world);
            }
            return clientCache;
        }
    }

    public static void clearClientCache() {
        clientCache = null;
    }

    public VoxelShape getWireShape(BlockPos pos) {
        return this.shapesByPos.getUnchecked(pos.immutable());
    }

//	@Override
//	public void load(CompoundTag nbt)
//	{
//		//noop
//	}

    @Override
    public CompoundTag save(CompoundTag compound) {
        return compound; //noop
    }

    public class VoxelLoader extends CacheLoader<BlockPos, VoxelShape> {

        @Override
        public VoxelShape load(BlockPos pos) throws Exception {
            Level world = VoxelCache.this.world;
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (!(block instanceof AbstractWireBlock wireBlock))
                return Shapes.empty();

            // fixed crash when voxel shape is empty
            final VoxelShape v = wireBlock.getCachedExpandedShapeVoxel(state, world, pos);
            if (v.isEmpty())
                return Shapes.box(7D, 7D, 7D, 9D, 9D, 9D);
            return v;
        }

    }
}
