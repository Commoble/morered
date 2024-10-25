package net.commoble.morered.transportation;

import net.commoble.morered.CommonTags;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class ExtractorBlock extends Block
{
	// output is current facing, input is face.getOpposite()
	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	protected final VoxelShape[] shapes;

	public ExtractorBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(POWERED, false));
		this.shapes = this.makeShapes();
	}

	@Override
	public void neighborChanged(BlockState thisState, Level level, BlockPos thisPos, Block neighborBlock, BlockPos neighborPos, boolean isMoving)
	{
		if (!level.isClientSide())
		{
			boolean isReceivingPower = level.hasNeighborSignal(thisPos);
			boolean isStatePowered = thisState.getValue(POWERED);
			if (isReceivingPower != isStatePowered)
			{
				if (isReceivingPower)
				{
					this.transferItem(thisState, thisPos, level);
					level.playSound(null, thisPos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.3F, level.random.nextFloat() * 0.1F + 0.8F);
				}
				else
				{
					level.playSound(null, thisPos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.1F, level.random.nextFloat() * 0.1F + 0.9F);
				}
				level.setBlock(thisPos, thisState.setValue(POWERED, Boolean.valueOf(isReceivingPower)), 2);
			}

		}
	}
	
	private void transferItem(BlockState state, BlockPos pos, Level level)
	{
		Direction outputDir = state.getValue(FACING);
		BlockPos outputPos = pos.relative(outputDir);
		Direction inputDir = outputDir.getOpposite();
		BlockPos inputPos = pos.relative(inputDir);

		IItemHandler inputHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, outputDir);
		if (inputHandler != null)
		{
			IItemHandler outputHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, outputPos, inputDir);
			
			// if the input handler exists and either the output handler exists or we have room to eject the item
			if (outputHandler != null || !level.getBlockState(outputPos).isCollisionShapeFullBlock(level, outputPos))
			{
				ItemStack stack = this.extractNextStack(inputHandler);
				if (stack.getCount() > 0)
				{
					ItemStack remaining = outputHandler == null ? stack : this.putStackInHandler(stack, outputHandler);
					WorldHelper.ejectItemstack(level, pos, outputDir, remaining);
				}
			}
		}
	}
	
	private ItemStack extractNextStack(IItemHandler handler)
	{
		int slots = handler.getSlots();
		for (int i=0; i<slots; i++)
		{
			ItemStack stack = handler.extractItem(i, 64, false);
			if (stack.getCount() > 0)
			{
				return stack.copy();
			}
		}
		return ItemStack.EMPTY;
	}
	
	private ItemStack putStackInHandler(ItemStack stack, IItemHandler handler)
	{
		ItemStack remaining = stack.copy();
		int slots = handler.getSlots();
		for (int i=0; i<slots; i++)
		{
			remaining = handler.insertItem(i, remaining, false);
			if (remaining.getCount() <= 0)
			{
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	//// facing and blockstate boilerplate

	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return this.defaultBlockState().setValue(FACING, WorldHelper.getBlockFacingForPlacement(context).getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot)
	{
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Deprecated
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, POWERED);
	}

	// model shapes

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		return this.shapes[this.getShapeIndex(state)];
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
	{
		if (stack.is(CommonTags.Items.WRENCHES))
		{
			level.setBlock(pos, state.cycle(FACING), UPDATE_ALL);
			return ItemInteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}
	

	public int getShapeIndex(BlockState state)
	{
		return state.getValue(FACING).ordinal();
	}

	protected VoxelShape[] makeShapes()
	{
		VoxelShape[] shapes = new VoxelShape[6];

		for (int face = 0; face < 6; face++) // dunswe
		{
			boolean DOWN = face == 0;
			boolean UP = face == 1;
			boolean NORTH = face == 2;
			boolean SOUTH = face == 3;
			boolean WEST = face == 4;
			boolean EAST = face == 5;
			
			// north==0, south==16

			double input_x_min = WEST ? 10D : 0D;
			double input_x_max = EAST ? 6D : 16D;
			double input_y_min = DOWN ? 10D : 0D;
			double input_y_max = UP ? 6D : 16D;
			double input_z_min = NORTH ? 10D : 0D;
			double input_z_max = SOUTH ? 6D : 16D;

			double mid_x_min = EAST ? 6D : 4D;
			double mid_x_max = WEST ? 10D : 12D;
			double mid_y_min = UP ? 6D : 4D;
			double mid_y_max = DOWN ? 10D : 12D;
			double mid_z_min = SOUTH ? 6D : 4D;
			double mid_z_max = NORTH ? 10D : 12D;

			double output_x_min = WEST ? 0D : EAST ? 12D : 6D;
			double output_x_max = WEST ? 4D : EAST ? 16D : 10D;
			double output_y_min = DOWN ? 0D : UP ? 12D : 6D;
			double output_y_max = DOWN ? 4D : UP ? 16D : 10D;
			double output_z_min = SOUTH ? 12D : NORTH ? 0D : 6D;
			double output_z_max = SOUTH ? 16D : NORTH ? 4D : 10D;

			VoxelShape input = Block.box(input_x_min, input_y_min, input_z_min, input_x_max, input_y_max,
					input_z_max);
			VoxelShape mid = Block.box(mid_x_min, mid_y_min, mid_z_min, mid_x_max, mid_y_max, mid_z_max);
			VoxelShape output = Block.box(output_x_min, output_y_min, output_z_min, output_x_max,
					output_y_max, output_z_max);

			shapes[face] = Shapes.or(input, mid, output);
		}

		return shapes;
	}
}
