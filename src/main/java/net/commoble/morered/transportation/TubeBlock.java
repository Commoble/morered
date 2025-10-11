package net.commoble.morered.transportation;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.commoble.morered.util.EightGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

public class TubeBlock extends Block implements SimpleWaterloggedBlock, EntityBlock
{
	public static final Direction[] FACING_VALUES = Direction.values();

	public static final BooleanProperty DOWN = PipeBlock.DOWN;
	public static final BooleanProperty UP = PipeBlock.UP;
	public static final BooleanProperty NORTH = PipeBlock.NORTH;
	public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
	public static final BooleanProperty WEST = PipeBlock.WEST;
	public static final BooleanProperty EAST = PipeBlock.EAST;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<OctahedralGroup> GROUP = EightGroup.TRANSFORM;

	public static final VoxelShape[] SHAPES = makeShapes();

	private static final BooleanProperty[] DIRECTION_PROPERTIES = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
	public static BooleanProperty getPropertyForDirection(Direction dir)
	{
		return DIRECTION_PROPERTIES[dir.ordinal()];
	}

	/** Texture location for rendering long tubes **/
	public final ResourceLocation textureLocation;

	public TubeBlock(ResourceLocation textureLocation, Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(NORTH, false)
			.setValue(EAST, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false)
			.setValue(DOWN, false)
			.setValue(UP, false)
			.setValue(WATERLOGGED, false)
			.setValue(GROUP, OctahedralGroup.IDENTITY));
		this.textureLocation = textureLocation;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED, GROUP);
	}

	/// basic block properties

	@Override
	public boolean isPathfindable(BlockState state, PathComputationType type)
	{
		return false;
	}

	// block behaviour
	@Override
	public void onPlace(BlockState newState, Level level, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		if (level instanceof ServerLevel serverLevel && !newState.is(oldState.getBlock()))
		{
			TubesInChunk.updateTubeSet(serverLevel, pos, Set<BlockPos>::add);
		}
		super.onPlace(newState, level, pos, oldState, isMoving);
	}

	/**
	 * Called when a neighboring block was changed and marks that this state should perform any checks during a neighbor change. Cases may include when redstone power is updated,
	 * cactus blocks popping off due to a neighboring solid block, etc.
	 */
	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, Orientation orientation, boolean isMoving)
	{
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TubeBlockEntity tube)
		{
			tube.onPossibleNetworkUpdateRequired();
		}
		super.neighborChanged(state, level, pos, blockIn, orientation, isMoving);
	}

	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place logic
	 */
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
	{
		if (!world.isClientSide() && world.getBlockEntity(pos) instanceof TubeBlockEntity te)
		{
			te.onPossibleNetworkUpdateRequired();
		}
		super.setPlacedBy(world, pos, state, placer, stack);
	}

	/// connections and states

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		BlockGetter world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		FluidState fluidstate = world.getFluidState(pos);
		return super.getStateForPlacement(context).setValue(DOWN, this.canConnectTo(world, pos, Direction.DOWN)).setValue(UP, this.canConnectTo(world, pos, Direction.UP))
			.setValue(NORTH, this.canConnectTo(world, pos, Direction.NORTH)).setValue(SOUTH, this.canConnectTo(world, pos, Direction.SOUTH))
			.setValue(WEST, this.canConnectTo(world, pos, Direction.WEST)).setValue(EAST, this.canConnectTo(world, pos, Direction.EAST))
			.setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
	}

	protected boolean canConnectTo(BlockGetter level, BlockPos pos, Direction face)
	{
		BlockPos newPos = pos.relative(face);
		BlockState state = level.getBlockState(newPos);
		Block newBlock = state.getBlock();
		BlockEntity blockEntity = level.getBlockEntity(newPos);
		if (newBlock instanceof TubeBlock tube && blockEntity instanceof TubeBlockEntity tubeEntity)
		{
			return this.isTubeCompatible(tube) && !tubeEntity.hasRemoteConnection(face.getOpposite());
		}

		if (newBlock instanceof LoaderBlock && state.getValue(LoaderBlock.FACING).equals(face.getOpposite()))
			return true; // todo make this configurable for arbitrary blocks instead of hardcoded

		if (newBlock instanceof ExtractorBlock && state.getValue(ExtractorBlock.ATTACHMENT_DIRECTION).equals(face.getOpposite()))
			return true;

		if (newBlock instanceof AbstractFilterBlock && state.getValue(AbstractFilterBlock.FACING).equals(face.getOpposite()))
			return true;

		if (level instanceof Level l && l.getCapability(Capabilities.Item.BLOCK, newPos, face.getOpposite()) != null)
		{
			return true;
		}
		return false;
	}

	public boolean isTubeCompatible(TubeBlock tube)
	{
		return true;
	}

	/**
	 * 
	 * @param state
	 * @return TRUE if the state is a tube block with a valid TileEntity and has neither an adjacent connection or remote connection on the given side, FALSE otherwise
	 */
	public static boolean hasOpenConnection(Level level, BlockPos pos, BlockState state, Direction face)
	{
		if (state.getBlock() instanceof TubeBlock tubeBlock)
		{
			return !tubeBlock.hasConnectionOnSide(state, face) && level.getBlockEntity(pos) instanceof TubeBlockEntity tube && !tube.hasRemoteConnection(face);
		}
		else
		{
			return false;
		}
	}

	public static Set<Direction> getConnectedDirections(BlockState state)
	{
		Block block = state.getBlock();
		Set<Direction> dirs = new HashSet<Direction>();
		if (block instanceof TubeBlock tubeBlock)
		{
			for (Direction dir : Direction.values())
			{
				if (tubeBlock.hasConnectionOnSide(state, dir))
				{
					dirs.add(dir);
				}
			}
		}
		return dirs;
	}

	public static Collection<RemoteConnection> getRemoteConnections(Level level, BlockPos pos)
	{
		return level.getBlockEntity(pos) instanceof TubeBlockEntity tube ? tube.getRemoteConnections().values() : ImmutableList.of();
	}

	/**
	 * Update the provided state given the provided neighbor facing and neighbor state, returning a new state. For example, fences make their connections to the passed in state if
	 * possible, and wet concrete powder immediately returns its solidified counterpart. Note that this method should ideally consider only the specific face passed in.
	 *
	 * @param facingState
	 *            The state that is currently at the position offset of the provided face to the stateIn at currentPos
	 */
	@Override
	public BlockState updateShape(BlockState thisState, LevelReader level, ScheduledTickAccess ticker, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random)
	{
		if (thisState.getValue(WATERLOGGED))
		{
			ticker.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return thisState.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(facing), Boolean.valueOf(this.canConnectTo(level, currentPos, facing)));
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

		// 6 different state flags = 2^6 = 64 different state models (waterlogging
		// doesn't affect model)
		VoxelShape[] shapes = new VoxelShape[64];

		// define the shapes for the piping core and the dunswe pipe segments
		// reminder: north = negative
		VoxelShape core = Block.box(ONE_QUARTER, ONE_QUARTER, ONE_QUARTER, THREE_QUARTERS, THREE_QUARTERS, THREE_QUARTERS);

		VoxelShape down = Block.box(SIX_SIXTEENTHS, MIN_VOXEL, SIX_SIXTEENTHS, TEN_SIXTEENTHS, THREE_QUARTERS, TEN_SIXTEENTHS);
		VoxelShape up = Block.box(SIX_SIXTEENTHS, THREE_QUARTERS, SIX_SIXTEENTHS, TEN_SIXTEENTHS, MAX_VOXEL, TEN_SIXTEENTHS);
		VoxelShape north = Block.box(SIX_SIXTEENTHS, SIX_SIXTEENTHS, MIN_VOXEL, TEN_SIXTEENTHS, TEN_SIXTEENTHS, ONE_QUARTER);
		VoxelShape south = Block.box(SIX_SIXTEENTHS, SIX_SIXTEENTHS, THREE_QUARTERS, TEN_SIXTEENTHS, TEN_SIXTEENTHS, MAX_VOXEL);
		VoxelShape west = Block.box(MIN_VOXEL, SIX_SIXTEENTHS, SIX_SIXTEENTHS, THREE_QUARTERS, TEN_SIXTEENTHS, TEN_SIXTEENTHS);
		VoxelShape east = Block.box(THREE_QUARTERS, SIX_SIXTEENTHS, SIX_SIXTEENTHS, MAX_VOXEL, TEN_SIXTEENTHS, TEN_SIXTEENTHS);

		VoxelShape[] dunswe = { down, up, north, south, west, east };

		for (int i = 0; i < 64; i++)
		{
			shapes[i] = core;
			// if the state flag exists in this state's 6-bit binary pattern, use the pipe
			// segment in this state model
			// down = LSB, east = MSB
			for (int j = 0; j < 6; j++)
			{
				if ((i & (1 << j)) != 0)
				{
					shapes[i] = Shapes.or(shapes[i], dunswe[j]);
				}
			}
		}

		return shapes;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		return SHAPES[this.getShapeIndex(state)];
	}

	public boolean hasConnectionOnSide(BlockState tubeState, Direction side)
	{
		return tubeState.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
	}

	public int getShapeIndex(BlockState state)
	{
		int index = 0;

		for (int j = 0; j < FACING_VALUES.length; ++j)
		{
			if (this.hasConnectionOnSide(state, FACING_VALUES[j]))
			{
				index |= 1 << j;
			}
		}

		return index;
	}

	/// watterloggy stuff

	@Override
	@Deprecated
	public FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	// entityblock stuff

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.TUBE_BLOCK_ENTITY.get().create(pos, state);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return type == MoreRed.TUBE_BLOCK_ENTITY.get() ? (BlockEntityTicker<T>) TubeBlockEntity.TICKER : EntityBlock.super.getTicker(level, state, type);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation)
	{
		BlockState newState = state;
		// get rotated versions of the six directional boolean states and then set them
		Set<Direction> newDirections = EnumSet.noneOf(Direction.class);
		for (int i=0; i<4; i++)
		{
			Direction dir = Direction.from2DDataValue(i);
			BooleanProperty prop = getPropertyForDirection(dir);
			Direction rotatedDir = rotation.rotate(dir);
			if (state.getValue(prop))
			{
				newDirections.add(rotatedDir);
			}
		}
		for (int i=0; i<4; i++)
		{
			Direction dir = Direction.from2DDataValue(i);
			BooleanProperty prop = getPropertyForDirection(dir);
			newState = newState.setValue(prop, newDirections.contains(dir));
		}
		return EightGroup.rotate(newState, rotation);
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		// get rotated versions of the six directional boolean states and then set them
		BlockState newState = state;
		Set<Direction> newDirections = EnumSet.noneOf(Direction.class);
		for (int i=0; i<4; i++)
		{
			Direction dir = Direction.from2DDataValue(i);
			BooleanProperty prop = getPropertyForDirection(dir);
			Direction mirroredDir = mirror.mirror(dir);
			if (state.getValue(prop))
			{
				newDirections.add(mirroredDir);
			}
		}
		for (int i=0; i<4; i++)
		{
			Direction dir = Direction.from2DDataValue(i);
			BooleanProperty prop = getPropertyForDirection(dir);
			newState = newState.setValue(prop, newDirections.contains(dir));
		}
		
		return EightGroup.mirror(newState, mirror);
	}
	
}
