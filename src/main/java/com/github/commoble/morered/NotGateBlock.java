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
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class NotGateBlock extends Block
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = GateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = GateBlockStateProperties.ROTATION;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public static final int OUTPUT_STRENGTH = 15;
	public static final int TICK_DELAY = 1;

	public static final VoxelShape[] SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.makeCuboidShape(0, 0, 0, 16, 2, 16), Block.makeCuboidShape(0, 14, 0, 16, 16, 16), Block.makeCuboidShape(0, 0, 0, 16, 16, 2),
		Block.makeCuboidShape(0, 0, 14, 16, 16, 16), Block.makeCuboidShape(0, 0, 0, 2, 16, 16), Block.makeCuboidShape(14, 0, 0, 16, 16, 16) };

	public NotGateBlock(Properties properties)
	{
		super(properties);
		this.setDefaultState(this.getStateContainer().getBaseState().with(ATTACHMENT_DIRECTION, Direction.DOWN).with(ROTATION, 0).with(POWERED, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		if (state.has(ATTACHMENT_DIRECTION))
		{
			return SHAPES_BY_DIRECTION[state.get(ATTACHMENT_DIRECTION).ordinal()];
		}
		else
		{
			return SHAPES_BY_DIRECTION[0];
		}
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		BlockState baseState = GateBlockStateProperties.getStateForPlacedGatePlate(this.getDefaultState(), context);
		boolean startPowered = this.hasInputPower(context.getWorld(), baseState, context.getPos());
		return baseState.with(POWERED, startPowered);
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
	{
		builder.add(ATTACHMENT_DIRECTION, ROTATION, POWERED);
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
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		super.onBlockAdded(state, worldIn, pos, oldState, isMoving);
		this.notifyNeighbors(worldIn, pos, state);
	}

	@Override
	@Deprecated
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!isMoving && state.getBlock() != newState.getBlock())
		{
			super.onReplaced(state, worldIn, pos, newState, isMoving);
			this.notifyNeighbors(worldIn, pos, state);
		}
	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		boolean wasPowered = state.get(POWERED);
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

		boolean wasPowered = state.get(POWERED);
		boolean isPowered = this.hasInputPower(worldIn, state, pos);
		if (wasPowered && !isPowered) // the input state changed, change the blockstate
		{
			worldIn.setBlockState(pos, state.with(POWERED, false), 2);
		}
		else if (!wasPowered)
		{
			worldIn.setBlockState(pos, state.with(POWERED, true), 2);
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
	 *             {@link IBlockState#getStrongPower(IBlockAccess,BlockPos,EnumFacing)}
	 *             whenever possible. Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public int getStrongPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
	{
		return blockState.getWeakPower(blockAccess, pos, side);
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
		if (!blockState.get(POWERED) && GateBlockStateProperties.getOutputDirection(blockState) == sideOfAdjacentBlock.getOpposite())
		{
			return OUTPUT_STRENGTH;
		}
		else
		{
			return 0;
		}
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
	
	public void notifyNeighbors(World world, BlockPos pos, BlockState state)
	{
		Direction outputDirection = GateBlockStateProperties.getOutputDirection(state);
		BlockPos outputPos = pos.offset(outputDirection);
		if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), java.util.EnumSet.of(outputDirection), false).isCanceled())
		{
			world.neighborChanged(outputPos, this, pos);
			world.notifyNeighborsOfStateExcept(outputPos, this, outputDirection);
		}
	}

	public Direction getInputDirection(BlockState state)
	{
		return GateBlockStateProperties.getOutputDirection(state).getOpposite();
	}

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

	/**
	 * Returns the blockstate with the given rotation from the passed blockstate. If
	 * inapplicable, returns the passed blockstate.
	 * 
	 * @deprecated call via {@link IBlockState#withRotation(Rotation)} whenever
	 *             possible. Implementing/overriding is fine.
	 */
	@Override
	@Deprecated
	public BlockState rotate(BlockState state, Rotation rotation)
	{
		if (state.has(ATTACHMENT_DIRECTION) && state.has(ROTATION))
		{
			Direction attachmentDirection = state.get(ATTACHMENT_DIRECTION);
			int rotationIndex = state.get(ROTATION);

			Direction newAttachmentDirection = rotation.rotate(attachmentDirection);
			int newRotationIndex = BlockStateUtil.getRotatedRotation(attachmentDirection, rotationIndex, rotation);

			return state.with(ATTACHMENT_DIRECTION, newAttachmentDirection).with(ROTATION, newRotationIndex);
		}
		else
		{
			return state;
		}
	}

	/**
	 * Returns the blockstate with the given mirror of the passed blockstate. If
	 * inapplicable, returns the passed blockstate.
	 * 
	 * @deprecated call via {@link IBlockState#withMirror(Mirror)} whenever
	 *             possible. Implementing/overriding is fine.
	 */
	@Override
	@Deprecated
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		if (state.has(ATTACHMENT_DIRECTION) && state.has(ROTATION))
		{
			Direction attachmentDirection = state.get(ATTACHMENT_DIRECTION);
			int rotationIndex = state.get(ROTATION);

			Direction newAttachmentDirection = mirror.mirror(attachmentDirection);
			int newRotationIndex = BlockStateUtil.getMirroredRotation(attachmentDirection, rotationIndex, mirror);

			return state.with(ATTACHMENT_DIRECTION, newAttachmentDirection).with(ROTATION, newRotationIndex);
		}
		else
		{
			return state;
		}
	}
}
