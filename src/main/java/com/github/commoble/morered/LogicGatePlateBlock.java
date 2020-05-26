package com.github.commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class LogicGatePlateBlock extends Block
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = GateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = GateBlockStateProperties.ROTATION;

	
	/** The first input clockwise from the output **/
	public static final BooleanProperty INPUT_A = GateBlockStateProperties.INPUT_A;
	/** The second input clockwise from the output **/
	public static final BooleanProperty INPUT_B = GateBlockStateProperties.INPUT_B;
	/** The third input clockwise from the output **/
	public static final BooleanProperty INPUT_C = GateBlockStateProperties.INPUT_B;
	
	public static final VoxelShape[] SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.makeCuboidShape(0, 0, 0, 16, 2, 16), Block.makeCuboidShape(0, 14, 0, 16, 16, 16), Block.makeCuboidShape(0, 0, 0, 16, 16, 2),
		Block.makeCuboidShape(0, 0, 14, 16, 16, 16), Block.makeCuboidShape(0, 0, 0, 2, 16, 16), Block.makeCuboidShape(14, 0, 0, 16, 16, 16) };
	
	public static final int OUTPUT_STRENGTH = 15;
	public static final int TICK_DELAY = 1;
	
	public LogicGatePlateBlock(Properties properties)
	{
		super(properties);
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
