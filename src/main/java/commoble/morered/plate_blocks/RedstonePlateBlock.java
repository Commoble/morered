package commoble.morered.plate_blocks;

import javax.annotation.Nullable;

import commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.common.Tags;

public abstract class RedstonePlateBlock extends PlateBlock
{
	public static final int OUTPUT_STRENGTH = 15;
	public static final int TICK_DELAY = 1;

	public RedstonePlateBlock(Properties properties)
	{
		super(properties);
	}
	
	public abstract InputSide[] getInputSides();
	public abstract void notifyNeighbors(Level world, BlockPos pos, BlockState state);
	
	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place
	 * logic
	 */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (this.hasInputPower(level, state, pos))
		{
			level.scheduleTick(pos, this, 1);
		}

	}
	
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = super.getStateForPlacement(context);
		for (InputSide side : this.getInputSides())
		{
			state = state.setValue(side.property, side.isBlockReceivingPower(world, state, pos));
		}
		return state;
	}
	@Override
	@Deprecated
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		boolean isPlayerHoldingStick = player.getItemInHand(hand).is(Tags.Items.RODS_WOODEN);
		
		// rotate the block when the player pokes it with a stick
		if (isPlayerHoldingStick && !level.isClientSide)
		{
			int newRotation = (state.getValue(ROTATION) + 1) % 4;
			BlockState newState = state.setValue(ROTATION, newRotation);
			for (InputSide side : this.getInputSides())
			{
				newState = newState.setValue(side.property, side.isBlockReceivingPower(level, newState, pos));
			}
			level.setBlockAndUpdate(pos, newState);
		}
		
		return isPlayerHoldingStick ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	@Deprecated
	@Override
	public boolean isSignalSource(BlockState state)
	{
		return true;
	}
	
	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side)
	{
		if (side == null)
			return false;
		
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(state);
		if (side == primaryOutputDirection.getOpposite())
			return true;
		
		// check input sides
		Direction attachmentDirection = state.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		int baseRotation = state.getValue(PlateBlockStateProperties.ROTATION);
		for (InputSide inputSide : this.getInputSides())
		{
			Direction inputDirection = BlockStateUtil.getInputDirection(attachmentDirection, baseRotation, inputSide.rotationsFromOutput);
			if (side == inputDirection.getOpposite())
				return true;
		}
		return false;
	}

	// Get the redstone power output that can be conducted indirectly through solid cubes
	@Deprecated
	@Override
	public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
	{
		return blockState.getSignal(blockAccess, pos, side);
	}
	
	@Override
	@Deprecated
	public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		super.onPlace(state, worldIn, pos, oldState, isMoving);
		this.notifyNeighbors(worldIn, pos, state);
	}
	
	@Override
	@Deprecated
	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!isMoving && state.getBlock() != newState.getBlock())
		{
			super.onRemove(state, worldIn, pos, newState, isMoving);
			this.notifyNeighbors(worldIn, pos, state);
		}
	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		// if any inputs changed, schedule a tick
		InputState oldInputState = InputState.getInput(state);
		InputState newInputState = InputState.getWorldPowerState(worldIn, state, pos);
		if (oldInputState != newInputState && !worldIn.getBlockTicks().willTickThisTick(pos, this))
		{
			// we have to have a 1-tick delay to avoid infinite loops
			worldIn.scheduleTick(pos, this, TICK_DELAY, TickPriority.HIGH);
		}
	}
	
	/**
	 * Return true if any of the three input directions are receiving power
	 * @param world The world the state is in
	 * @param state A blockstate of this block
	 * @param pos The position of the state in the world
	 * @return Whether any of the block's input directions are receiving power
	 */
	public boolean hasInputPower(Level world, BlockState state, BlockPos pos)
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

}
