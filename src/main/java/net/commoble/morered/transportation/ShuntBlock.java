package net.commoble.morered.transportation;

import net.commoble.morered.util.WorldHelper;
import net.commoble.morered.CommonTags;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShuntBlock extends Block implements EntityBlock
{
	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	
	public static final VoxelShape[] SHAPES = makeShapes();

	public ShuntBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	//// facing and blockstate boilerplate

	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return this.defaultBlockState().setValue(FACING, WorldHelper.getBlockFacingForPlacement(context));
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

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}
	
	/// model shapes

	public static VoxelShape[] makeShapes()
	{
		final double MIN_VOXEL = 0D;
		final double ONE_QUARTER = 4D;
		final double THREE_QUARTERS = 12D;
		final double SIX_SIXTEENTHS = 6D;
		final double TEN_SIXTEENTHS = 10D;
		final double MAX_VOXEL = 16D;
		
		// one set of shapes for each state * six directional states = 6 sets of shapes
		VoxelShape[] shapes = new VoxelShape[6];

		// define the shapes for the piping core and the dunswe pipe segments
		// reminder: north = negative
		
		// core voxels
		VoxelShape coreNorth = Block.box(ONE_QUARTER, ONE_QUARTER, MIN_VOXEL,
				THREE_QUARTERS, THREE_QUARTERS, THREE_QUARTERS);
		VoxelShape coreSouth = Block.box(ONE_QUARTER, ONE_QUARTER, ONE_QUARTER,
				THREE_QUARTERS, THREE_QUARTERS,	MAX_VOXEL);
		VoxelShape coreWest = Block.box(MIN_VOXEL, ONE_QUARTER, ONE_QUARTER,
				THREE_QUARTERS, THREE_QUARTERS, THREE_QUARTERS);
		VoxelShape coreEast = Block.box(ONE_QUARTER, ONE_QUARTER, ONE_QUARTER,
				MAX_VOXEL, THREE_QUARTERS, THREE_QUARTERS);
		VoxelShape coreDown = Block.box(ONE_QUARTER, MIN_VOXEL, ONE_QUARTER,
				THREE_QUARTERS, THREE_QUARTERS, THREE_QUARTERS);
		VoxelShape coreUp = Block.box(ONE_QUARTER, ONE_QUARTER, ONE_QUARTER,
				THREE_QUARTERS, MAX_VOXEL, THREE_QUARTERS);
		
		// tube voxels
		VoxelShape down = Block.box(SIX_SIXTEENTHS, MIN_VOXEL, SIX_SIXTEENTHS, TEN_SIXTEENTHS,
				THREE_QUARTERS, TEN_SIXTEENTHS);
		VoxelShape up = Block.box(SIX_SIXTEENTHS, THREE_QUARTERS, SIX_SIXTEENTHS, TEN_SIXTEENTHS, MAX_VOXEL,
				TEN_SIXTEENTHS);
		VoxelShape north = Block.box(SIX_SIXTEENTHS, SIX_SIXTEENTHS, MIN_VOXEL, TEN_SIXTEENTHS,
				TEN_SIXTEENTHS, ONE_QUARTER);
		VoxelShape south = Block.box(SIX_SIXTEENTHS, SIX_SIXTEENTHS, THREE_QUARTERS, TEN_SIXTEENTHS,
				TEN_SIXTEENTHS, MAX_VOXEL);
		VoxelShape west = Block.box(MIN_VOXEL, SIX_SIXTEENTHS, SIX_SIXTEENTHS, THREE_QUARTERS,
				TEN_SIXTEENTHS, TEN_SIXTEENTHS);
		VoxelShape east = Block.box(THREE_QUARTERS, SIX_SIXTEENTHS, SIX_SIXTEENTHS, MAX_VOXEL,
				TEN_SIXTEENTHS, TEN_SIXTEENTHS);
		
		VoxelShape[] tube_dunswe = { down, up, north, south, west, east };
		VoxelShape[] core_dunswe = { coreDown, coreUp, coreNorth, coreSouth, coreWest, coreEast};

		for (int state_dir = 0; state_dir < 6; state_dir++)
		{
			VoxelShape stateShape = core_dunswe[state_dir];
			for (int voxel_dir=0; voxel_dir<6; voxel_dir++)
			{
				if (voxel_dir != state_dir)
				{
					stateShape = Shapes.or(stateShape, tube_dunswe[voxel_dir]);
				}
			}
			shapes[state_dir] = stateShape;
		}

		return shapes;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES[this.getShapeIndex(state)];
	}

	public int getShapeIndex(BlockState state)
	{
		return state.getValue(FACING).ordinal();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().shuntEntity.get().create(pos, state);
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
}
