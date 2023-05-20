package commoble.morered.wires;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class VoxelCache extends SavedData
{
	public static final String ID = "morered:voxelcache";
	private static VoxelCache clientCache = null;
	
	protected final Level world;
	public final LoadingCache<BlockPos, VoxelShape> shapesByPos;
	
	public VoxelCache(@Nonnull Level world)
	{
		this.world = world;
		this.shapesByPos = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.MINUTES)
			.build(new VoxelLoader());
	}
	
	public static VoxelCache get(@Nonnull Level level)
	{
		if (level instanceof ServerLevel serverLevel)
		{
			// data is transient, don't need to load anything
			return serverLevel.getDataStorage().computeIfAbsent(tag -> new VoxelCache(level), () -> new VoxelCache(level), ID);
		}
		else
		{
			if (clientCache == null || clientCache.world != level)
			{
				clientCache = new VoxelCache(level);
			}
			return clientCache;
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag)
	{
		return tag; //noop
	}
	
	public static void clearClientCache()
	{
		clientCache = null;
	}
	
	public VoxelShape getWireShape(BlockPos pos)
	{
		return this.shapesByPos.getUnchecked(pos.immutable());
	}
	
	public class VoxelLoader extends CacheLoader<BlockPos, VoxelShape>
	{

		@Override
		public VoxelShape load(BlockPos pos) throws Exception
		{
			Level world = VoxelCache.this.world;
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (!(block instanceof AbstractWireBlock))
				return Shapes.empty();
			
			AbstractWireBlock wireBlock = (AbstractWireBlock)block;
			
			return wireBlock.getCachedExpandedShapeVoxel(state,world,pos);
		}
		
	}
}
