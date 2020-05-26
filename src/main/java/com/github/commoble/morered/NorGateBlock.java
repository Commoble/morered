package com.github.commoble.morered;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class NorGateBlock extends LogicGatePlateBlock
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = GateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = GateBlockStateProperties.ROTATION;
	public static final BooleanProperty INPUT_A = GateBlockStateProperties.INPUT_A;
	public static final BooleanProperty INPUT_B = GateBlockStateProperties.INPUT_B;
	public static final BooleanProperty INPUT_C = GateBlockStateProperties.INPUT_C;

	public NorGateBlock(Properties properties)
	{
		super(properties);
		this.setDefaultState(this.getStateContainer().getBaseState()
			.with(ATTACHMENT_DIRECTION, Direction.DOWN)
			.with(ROTATION, 0)
			.with(INPUT_A, false)
			.with(INPUT_B, false)
			.with(INPUT_C, false)
			);
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
	{
		builder.add(ATTACHMENT_DIRECTION, ROTATION, INPUT_A, INPUT_B, INPUT_C);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getPos();
		BlockState state = GateBlockStateProperties.getStateForPlacedGatePlate(this.getDefaultState(), context);
		for (InputSide side : InputSide.values())
		{
			state = state.with(side.property, side.isBlockReceivingPower(world, state, pos));
		}
		return state;
	}

	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place
	 * logic
	 */
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (this.hasInputPower(worldIn, state, pos))
		{
			worldIn.getPendingBlockTicks().scheduleTick(pos, this, 1);
		}

	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		// if any inputs changed, schedule a tick
		InputState oldInputState = InputState.getPreviousState(state);
		InputState newInputState = InputState.getWorldPowerState(worldIn, state, pos);
		if (oldInputState != newInputState && !worldIn.getPendingBlockTicks().isTickPending(pos, this))
		{
			worldIn.getPendingBlockTicks().scheduleTick(pos, this, TICK_DELAY, TickPriority.HIGH);
		}
	}

	@Override
	public void tick(BlockState oldBlockState, ServerWorld world, BlockPos pos, Random rand)
	{
		BlockState newBlockState = InputState.getUpdatedBlockState(world, oldBlockState, pos);
		if (newBlockState != oldBlockState)
		{
			world.setBlockState(pos, newBlockState, 2);
		}
	}

	/**
	 * @deprecated call via
	 *             {@link IBlockState#getWeakPower(IBlockAccess,BlockPos,EnumFacing)}
	 *             whenever possible. Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
	{
		if (!(blockState.get(INPUT_A) || blockState.get(INPUT_B) || blockState.get(INPUT_C))
			&& GateBlockStateProperties.getOutputDirection(blockState) == sideOfAdjacentBlock.getOpposite())
		{
			return OUTPUT_STRENGTH;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Return true if any of the three input directions are receiving power
	 * @param world
	 * @param state
	 * @param pos
	 * @return
	 */
	public boolean hasInputPower(World world, BlockState state, BlockPos pos)
	{
		InputSide[] sides = InputSide.values();
		for (InputSide side : sides)
		{
			if (side.isBlockReceivingPower(world, state, pos))
			{
				return true;
			}
		}
		return false;
	}

}
