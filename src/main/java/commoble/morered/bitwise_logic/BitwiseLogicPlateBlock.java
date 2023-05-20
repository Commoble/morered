package commoble.morered.bitwise_logic;

import javax.annotation.Nonnull;

import commoble.morered.MoreRed;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.plate_blocks.PlateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.common.Tags;

public abstract class BitwiseLogicPlateBlock extends PlateBlock implements EntityBlock
{
	public static final int TICK_DELAY = 1;
	public static final VoxelShape[] DOUBLE_PLATE_SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.box(0, 0, 0, 16, 4, 16),
		Block.box(0, 12, 0, 16, 16, 16),
		Block.box(0, 0, 0, 16, 16, 4),
		Block.box(0, 0, 12, 16, 16, 16),
		Block.box(0, 0, 0, 4, 16, 16),
		Block.box(12, 0, 0, 16, 16, 16) };
	public static final ChanneledPowerSupplier NO_POWER_SUPPLIER = (world,pos,state,dir,channel)->0;
	
	protected abstract void updatePower(Level world, BlockPos thisPos, BlockState thisState);
	public abstract boolean canConnectToAdjacentCable(@Nonnull BlockGetter world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire);

	public BitwiseLogicPlateBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.instance().bitwiseLogicGateBeType.get().create(pos, state);
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		if (state.hasProperty(ATTACHMENT_DIRECTION))
		{
			return DOUBLE_PLATE_SHAPES_BY_DIRECTION[state.getValue(ATTACHMENT_DIRECTION).ordinal()];
		}
		else
		{
			return DOUBLE_PLATE_SHAPES_BY_DIRECTION[0];
		}
	}
	
	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place
	 * logic
	 */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		level.scheduleTick(pos, this, TICK_DELAY);
	}
	
	// forge hook, signals that a neighboring block's TE data or comparator data updated
	@Override
	public void onNeighborChange(BlockState thisState, LevelReader levelReader, BlockPos thisPos, BlockPos neighborPos)
	{
		super.onNeighborChange(thisState, levelReader, thisPos, neighborPos);
		if (levelReader instanceof Level level)
		{
			level.scheduleTick(thisPos, this, TICK_DELAY, TickPriority.HIGH);
		}
		
	}

	@Override
	@Deprecated
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
	{
		// if this is just a blockstate change, make sure we tell the TE to invalidate and reset its capability
		if (state.getBlock() == newState.getBlock())
		{
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof ChanneledPowerStorageBlockEntity powerBe)
			{
				powerBe.resetCapabilities();
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}
	
	@Override
	@Deprecated
	public void tick(BlockState oldBlockState, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		this.updatePower(level,pos,oldBlockState);
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
			level.setBlockAndUpdate(pos, newState);
		}
		
		return isPlayerHoldingStick ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

}
