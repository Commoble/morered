package commoble.morered.plate_blocks;

import javax.annotation.Nullable;

import commoble.morered.util.BlockStateUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
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
	public abstract void notifyNeighbors(World world, BlockPos pos, BlockState state);
	
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
		BlockState state = super.getStateForPlacement(context);
		for (InputSide side : this.getInputSides())
		{
			state = state.with(side.property, side.isBlockReceivingPower(world, state, pos));
		}
		return state;
	}
	
	public static final ITag<Item> STICK = Tags.Items.RODS_WOODEN;//ItemTags.Wrapper STICK = new ItemTags.Wrapper(new ResourceLocation("forge:rods/wooden"));
	
	@Override
	@Deprecated
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit)
	{
		boolean isPlayerHoldingStick = STICK.contains(player.getHeldItem(handIn).getItem());
		
		// rotate the block when the player pokes it with a stick
		if (isPlayerHoldingStick && !worldIn.isRemote)
		{
			int newRotation = (state.get(ROTATION) + 1) % 4;
			BlockState newState = state.with(ROTATION, newRotation);
			for (InputSide side : this.getInputSides())
			{
				newState = newState.with(side.property, side.isBlockReceivingPower(worldIn, newState, pos));
			}
			worldIn.setBlockState(pos, newState);
		}
		
		return isPlayerHoldingStick ? ActionResultType.SUCCESS : ActionResultType.PASS;
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
	
	@Override
	public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, Direction side)
	{
		if (side == null)
			return false;
		
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(state);
		if (side == primaryOutputDirection.getOpposite())
			return true;
		
		// check input sides
		Direction attachmentDirection = state.get(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		int baseRotation = state.get(PlateBlockStateProperties.ROTATION);
		for (InputSide inputSide : this.getInputSides())
		{
			Direction inputDirection = BlockStateUtil.getInputDirection(attachmentDirection, baseRotation, inputSide.rotationsFromOutput);
			if (side == inputDirection.getOpposite())
				return true;
		}
		return false;
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
			// we have to have a 1-tick delay to avoid infinite loops
			worldIn.getPendingBlockTicks().scheduleTick(pos, this, TICK_DELAY, TickPriority.HIGH);
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

}
