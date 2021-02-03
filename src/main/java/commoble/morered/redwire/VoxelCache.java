package commoble.morered.redwire;

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
	
	public static final LoadingCache<Integer, VoxelShape> SHAPE_CACHE = CacheBuilder.newBuilder()
		.expireAfterAccess(5, TimeUnit.MINUTES)
		.build(new CacheLoader<Integer, VoxelShape>()
		{

			@Override
			public VoxelShape load(Integer key) throws Exception
			{
				return WireBlock.makeExpandedShapeForIndex(key);
			}
		
		});
	
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
			return ((ServerWorld)world).getSavedData().getOrCreate(() -> new VoxelCache(world), ID);
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
		return this.shapesByPos.getUnchecked(pos);
	}

	@Override
	public void read(CompoundNBT nbt)
	{
		//noop
	}

	@Override
	public CompoundNBT write(CompoundNBT compound)
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
			if (!(block instanceof WireBlock))
				return VoxelShapes.empty();
			
			int index = ((WireBlock)block).getExpandedShapeIndex(state, world, pos);
			
			return SHAPE_CACHE.getUnchecked(index);
		}
		
	}
}
