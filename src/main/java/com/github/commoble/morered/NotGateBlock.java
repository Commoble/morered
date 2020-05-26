package com.github.commoble.morered;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class NotGateBlock extends LogicGatePlateBlock
{
	public static final BooleanProperty INPUT_B = LogicGatePlateBlock.INPUT_B;

	public NotGateBlock(Properties properties)
	{
		super(properties);
		this.setDefaultState(this.getStateContainer().getBaseState().with(ATTACHMENT_DIRECTION, Direction.DOWN).with(ROTATION, 0).with(INPUT_B, false));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
	{
		builder.add(ATTACHMENT_DIRECTION, ROTATION, INPUT_B);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		BlockState baseState = GateBlockStateProperties.getStateForPlacedGatePlate(this.getDefaultState(), context);
		boolean startPowered = this.hasInputPower(context.getWorld(), baseState, context.getPos());
		return baseState.with(INPUT_B, startPowered);
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
		boolean wasPowered = state.get(INPUT_B);
		boolean isPowered = this.hasInputPower(worldIn, state, pos);
		if (wasPowered != isPowered && !worldIn.getPendingBlockTicks().isTickPending(pos, this))
		{
			TickPriority tickpriority = TickPriority.HIGH;
			if (wasPowered)
			{
				tickpriority = TickPriority.VERY_HIGH;
			}

			worldIn.getPendingBlockTicks().scheduleTick(pos, this, TICK_DELAY, tickpriority);
		}
	}

	@Override
	public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand)
	{

		boolean wasPowered = state.get(INPUT_B);
		boolean isPowered = this.hasInputPower(worldIn, state, pos);
		if (wasPowered && !isPowered) // the input state changed, change the blockstate
		{
			worldIn.setBlockState(pos, state.with(INPUT_B, false), 2);
		}
		else if (!wasPowered)
		{
			worldIn.setBlockState(pos, state.with(INPUT_B, true), 2);
			if (!isPowered)
			{ // the tick originally scheduled us while receiving input power, but we are no
				// longer receiving power
				// propagate the pulse for 1 tick, and schedule another tick
				worldIn.getPendingBlockTicks().scheduleTick(pos, this, TICK_DELAY, TickPriority.VERY_HIGH);
			}
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
		// POWERED is input power, we have output power on the output side if we have no
		// input power
		if (!blockState.get(INPUT_B) && GateBlockStateProperties.getOutputDirection(blockState) == sideOfAdjacentBlock.getOpposite())
		{
			return OUTPUT_STRENGTH;
		}
		else
		{
			return 0;
		}
	}

//	public Direction getInputDirection(BlockState state)
//	{
//		return GateBlockStateProperties.getOutputDirection(state).getOpposite();
//	}

	public boolean hasInputPower(World world, BlockState state, BlockPos pos)
	{
		Direction outputDirection = GateBlockStateProperties.getOutputDirection(state);
		Direction inputDirection = outputDirection.getOpposite();
		BlockPos inputPos = pos.offset(inputDirection);

		int power = world.getRedstonePower(inputPos, inputDirection);
		if (power > 0)
		{
			return true;
		}
		else
		{
			BlockState inputState = world.getBlockState(inputPos);
			return (inputState.getBlock() == Blocks.REDSTONE_WIRE && inputState.get(RedstoneWireBlock.POWER) > 0);
		}
	}


}
