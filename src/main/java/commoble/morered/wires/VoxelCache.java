package commoble.morered.wires;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

public class VoxelCache extends WorldSavedData
{
	public static final String ID = "morered:voxelcache";
	private static VoxelCache clientCache = null;
	
	protected final World world;
	public final LoadingCache<BlockPos, VoxelShape> shapesByPos;
	
	public VoxelCache(@Nonnull World world)
	{
		super(ID);
		this.world = world;
		this.shapesByPos = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.MINUTES)
			.build(new VoxelLoader());
	}
	
	public static VoxelCache get(@Nonnull World world)
	{
		if (world instanceof ServerWorld)
		{
			return ((ServerWorld)world).getDataStorage().computeIfAbsent(() -> new VoxelCache(world), ID);
		}
		else
		{
			if (clientCache == null || clientCache.world != world)
			{
				clientCache = new VoxelCache(world);
			}
			return clientCache;
		}
	}
	
	public static void clearClientCache()
	{
		clientCache = null;
	}
	
	public VoxelShape getWireShape(BlockPos pos)
	{
		return this.shapesByPos.getUnchecked(pos.immutable());
	}

	@Override
	public void load(CompoundNBT nbt)
	{
		//noop
	}

	@Override
	public CompoundNBT save(CompoundNBT compound)
	{
		return compound; //noop
	}
	
	public class VoxelLoader extends CacheLoader<BlockPos, VoxelShape>
	{

		@Override
		public VoxelShape load(BlockPos pos) throws Exception
		{
			World world = VoxelCache.this.world;
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (!(block instanceof AbstractWireBlock))
				return VoxelShapes.empty();
			
			AbstractWireBlock wireBlock = (AbstractWireBlock)block;
			
			return wireBlock.getCachedExpandedShapeVoxel(state,world,pos);
		}
		
	}
}
