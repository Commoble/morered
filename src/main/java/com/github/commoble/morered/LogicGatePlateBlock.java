package com.github.commoble.morered;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
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

public abstract class LogicGatePlateBlock extends Block
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = GateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = GateBlockStateProperties.ROTATION;

	
	public static final VoxelShape[] SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.makeCuboidShape(0, 0, 0, 16, 2, 16), Block.makeCuboidShape(0, 14, 0, 16, 16, 16), Block.makeCuboidShape(0, 0, 0, 16, 16, 2),
		Block.makeCuboidShape(0, 0, 14, 16, 16, 16), Block.makeCuboidShape(0, 0, 0, 2, 16, 16), Block.makeCuboidShape(14, 0, 0, 16, 16, 16) };
	
	public static final int OUTPUT_STRENGTH = 15;
	public static final int TICK_DELAY = 1;
	
	@FunctionalInterface
	public static interface LogicGateBlockFactory
	{	
		public LogicGatePlateBlock makeBlock(LogicFunction function, Block.Properties properties);
	}
	
	public static final LogicGateBlockFactory THREE_INPUTS = getBlockFactory(InputSide.A, InputSide.B, InputSide.C);
	public static final LogicGateBlockFactory T_INPUTS = getBlockFactory(InputSide.A, InputSide.C);
	public static final LogicGateBlockFactory LINEAR_INPUT = getBlockFactory(InputSide.B);

	
	public static LogicGateBlockFactory getBlockFactory(InputSide... inputs)
	{
		return (properties, function) -> new LogicGatePlateBlock(function, properties)
		{
			// fillStateContainer in LogicGatePlateBlock needs to know which blockstate properties to use
			// but fillStateContainer gets called in the superconstructor, before any information about
			// our block is available.
			// The only safe way to handle this (aside from just making subclasses) is this
			// cursed closure class
			@Override
			public InputSide[] getInputSides()
			{
				return inputs;
			}
		};
	}
	
	private final LogicFunction function;
	
	public LogicGatePlateBlock(Properties properties, LogicFunction function)
	{
		super(properties);
		this.function = function;
		
		BlockState baseState = this.getStateContainer().getBaseState()
			.with(ATTACHMENT_DIRECTION, Direction.DOWN)
			.with(ROTATION, 0);
		
		for (InputSide side : this.getInputSides())
		{
			baseState = baseState.with(side.property, false);
		}
		
		this.setDefaultState(baseState);
	}
	
	/**
	 * We *have* to make this a method instead of a constructor arg,
	 * because fillStateContainer gets called in the superconstructor
	 * so we can't set any fields in this class before it gets called
	 * 
	 * @return The InputSides this block uses to get redstone input from
	 */
	public abstract InputSide[] getInputSides();

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
	{
		builder.add(ATTACHMENT_DIRECTION, ROTATION);
		for (InputSide side : this.getInputSides())
		{
			builder.add(side.property);
		}
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

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getPos();
		BlockState state = GateBlockStateProperties.getStateForPlacedGatePlate(this.getDefaultState(), context);
		for (InputSide side : this.getInputSides())
		{
			state = state.with(side.property, side.isBlockReceivingPower(world, state, pos));
		}
		return state;
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

	/**
	 * @deprecated call via
	 *             {@link IBlockState#getWeakPower(IBlockAccess,BlockPos,EnumFacing)}
	 *             whenever possible. Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction sideOfAdjacentBlock)
	{
		if (InputState.getInput(blockState).applyLogic(this.function)
			&& GateBlockStateProperties.getOutputDirection(blockState) == sideOfAdjacentBlock.getOpposite())
		{
			return OUTPUT_STRENGTH;
		}
		else
		{
			return 0;
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
		// if any inputs changed, schedule a tick
		InputState oldInputState = InputState.getInput(state);
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
	 * Return true if any of the three input directions are receiving power
	 * @param world
	 * @param state
	 * @param pos
	 * @return
	 */
	public boolean hasInputPower(World world, BlockState state, BlockPos pos)
	{
		for (InputSide side : this.getInputSides())
		{
			if (side.isBlockReceivingPower(world, state, pos))
			{
				return true;
			}
		}
		return false;
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
