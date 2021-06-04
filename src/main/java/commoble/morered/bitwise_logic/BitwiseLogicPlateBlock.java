package commoble.morered.bitwise_logic;

import java.util.Random;

import javax.annotation.Nonnull;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.plate_blocks.PlateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;

public abstract class BitwiseLogicPlateBlock extends PlateBlock
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
	
	protected abstract void updatePower(World world, BlockPos thisPos, BlockState thisState);
	public abstract boolean canConnectToAdjacentCable(@Nonnull IBlockReader world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire);

	public BitwiseLogicPlateBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return TileEntityRegistrar.BITWISE_LOGIC_PLATE.get().create();
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
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
	public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		worldIn.getBlockTicks().scheduleTick(pos, this, 1);
	}
	
	// forge hook, signals that a neighboring block's TE data or comparator data updated
	@Override
	public void onNeighborChange(BlockState thisState, IWorldReader world, BlockPos thisPos, BlockPos neighborPos)
	{
		super.onNeighborChange(thisState, world, thisPos, neighborPos);
		if (world instanceof World)
		{
			((World)world).getBlockTicks().scheduleTick(thisPos, this, TICK_DELAY, TickPriority.HIGH);
		}
		
	}

	@Override
	@Deprecated
	public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		// if this is just a blockstate change, make sure we tell the TE to invalidate and reset its capability
		if (state.getBlock() == newState.getBlock())
		{
			TileEntity te = worldIn.getBlockEntity(pos);
			if (te instanceof ChanneledPowerStorageTileEntity)
			{
				((ChanneledPowerStorageTileEntity)te).resetCapabilities();
			}
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}
	
	@Override
	public void tick(BlockState oldBlockState, ServerWorld world, BlockPos pos, Random rand)
	{
		this.updatePower(world,pos,oldBlockState);
	}
	
	@Override
	@Deprecated
	public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit)
	{
		boolean isPlayerHoldingStick = Tags.Items.RODS_WOODEN.contains(player.getItemInHand(handIn).getItem());
		
		// rotate the block when the player pokes it with a stick
		if (isPlayerHoldingStick && !worldIn.isClientSide)
		{
			int newRotation = (state.getValue(ROTATION) + 1) % 4;
			BlockState newState = state.setValue(ROTATION, newRotation);
			worldIn.setBlockAndUpdate(pos, newState);
		}
		
		return isPlayerHoldingStick ? ActionResultType.SUCCESS : ActionResultType.PASS;
	}

}
