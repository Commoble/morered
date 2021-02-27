package commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.util.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public abstract class AbstractPoweredWirePostBlock extends AbstractPostBlock
{
	public static final IntegerProperty POWER = BlockStateProperties.POWER_0_15;
	public static final EnumSet<Direction> NO_DIRECTIONS = EnumSet.noneOf(Direction.class);
	
	protected static final VoxelShape[] POST_SHAPES_DUNSWE = {
		Block.makeCuboidShape(6D, 0D, 6D, 10D, 10D, 10D),
		Block.makeCuboidShape(6D, 16D, 6D, 10D, 6D, 10D),
		Block.makeCuboidShape(6D, 6D, 0D, 10D, 10D, 10D),
		Block.makeCuboidShape(6D, 6D, 6D, 10D, 10D, 16D),
		Block.makeCuboidShape(0D, 6D, 6D, 10D, 10D, 10D),
		Block.makeCuboidShape(6D, 6D, 6D, 16D, 10D, 10D)
	};
	
	// function that gets the connected directions for a given blockstate
	private final Function<BlockState, EnumSet<Direction>> connectionGetter;

	public AbstractPoweredWirePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties);
		this.setDefaultState(this.getDefaultState()
			.with(POWER, 0));
		this.connectionGetter = connectionGetter;
	}
	
	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader reader)
	{
		return TileEntityRegistrar.REDWIRE_POST.get().create();
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
	{
		super.fillStateContainer(builder);
		builder.add(POWER);
	}
	
	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place
	 * logic
	 */
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		int oldPower = state.get(POWER);
		int newPower = this.getNewPower(state, world, pos);
		if (oldPower != newPower)
		{
			world.setBlockState(pos, state.with(POWER, newPower), 2);
		}

	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
		int oldPower = state.get(POWER);
		int newPower = this.getNewPower(state, world, pos);
		if (oldPower != newPower)
		{
			world.setBlockState(pos, state.with(POWER, newPower), 2);
		}

	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		BlockState attachmentState = super.getStateForPlacement(context);
		return attachmentState == null
			? null
			: attachmentState.with(POWER, this.getNewPower(attachmentState, context.getWorld(), context.getPos()));
	}

	/**
	 * Called after a block is placed next to this block
	 * 
	 * Update the provided state given the provided neighbor facing and neighbor
	 * state, returning a new state. For example, fences make their connections to
	 * the passed in state if possible, and wet concrete powder immediately returns
	 * its solidified counterpart. Note that this method should ideally consider
	 * only the specific face passed in.
	 */
	@Override
	public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
	{
		return state.with(POWER, this.getNewPower(state, world, pos));
	}

	/**
	 * Can this block provide power. Only wire currently seems to have this change
	 * based on its state.
	 * 
	 * @deprecated call via {@link IBlockState#canProvidePower()} whenever possible.
	 *             Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public boolean canProvidePower(BlockState state)
	{
		return true;
	}

	@Deprecated
	@Override
	public int getStrongPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
	{
		return blockState.getWeakPower(blockAccess, pos, side);
	}
	
	@Deprecated
	@Override
	public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction directionOfThisBlockFromCaller)
	{
		if (this.connectionGetter.apply(blockState).contains(directionOfThisBlockFromCaller.getOpposite()))
		{
			return blockState.get(POWER);
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Returns a power level equal to one less than the highest power level among the blocks that this block can connect to
	 * @param state
	 * @param world
	 * @param pos
	 * @return
	 */
	public int getNewPower(BlockState state, IWorld world, BlockPos pos)
	{
		return Math.max(0, Math.max(this.getNeighborPower(state, world, pos), this.getConnectionPower(state, world, pos)) -1);
	}
	
	/** Returns the highest redstone power level among the neighbors adjacent to this block's redstone-connecting sides **/
	public int getNeighborPower(BlockState state, IWorld world, BlockPos pos)
	{
		if (world instanceof World)
		{
			return this.connectionGetter.apply(state).stream()
				.map(direction -> ((World)world).getRedstonePower(pos.offset(direction), direction))
				.reduce(0, Math::max);
		}
		else
		{
			return 0;
		}
	}
	
	/** Returns the highest redstone power level among the posts connected to this post **/
	public int getConnectionPower(BlockState state, IWorld world, BlockPos pos)
	{
		return WorldHelper.getTileEntityAt(WirePostTileEntity.class, world, pos)
			.map(te -> te.getRemoteConnections())
			.orElse(ImmutableSet.of())
			.stream()
			.map(tePos -> world.getBlockState(tePos))
			.map(otherState -> otherState.hasProperty(POWER) ? otherState.get(POWER) : 0)
			.reduce(0, Math::max);
	}



	
	@Override
	public void notifyNeighbors(World world, BlockPos pos, BlockState state)
	{
		EnumSet<Direction> neighborDirections = this.connectionGetter.apply(state);
		if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), neighborDirections, false).isCanceled())
		{
			for (Direction dir : neighborDirections)
			{
				BlockPos neighborPos = pos.offset(dir);
				world.neighborChanged(neighborPos, this, pos);
				world.notifyNeighborsOfStateExcept(neighborPos, this, dir.getOpposite());
			}
			WorldHelper.getTileEntityAt(WirePostTileEntity.class, world, pos).ifPresent(te -> te.notifyConnections());
		}
	}

}
