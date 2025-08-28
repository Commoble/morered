package net.commoble.morered.plate_blocks;

import java.util.Map;

import net.commoble.exmachina.api.ExMachinaGameEvents;
import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.TwentyFourBlock;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class AlternatorBlock extends PlateBlock implements EntityBlock
{
	public static final IntegerProperty AXLE_ROTATION = IntegerProperty.create("axle_rotation", 0, 3);
	public static final double AXLE_1_THRESHOLD = Math.PI / 4D;	// less than this is 0, more is 1
	public static final double AXLE_2_THRESHOLD = 3D*AXLE_1_THRESHOLD; // less is 1, more is 2
	public static final double AXLE_3_THRESHOLD = 5D*AXLE_1_THRESHOLD; // less is 2, more is 3
	public static final double AXLE_0_THRESHOLD = 7D*AXLE_1_THRESHOLD; // less is 3, more is 0

	public AlternatorBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(AXLE_ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(AXLE_ROTATION);
	}

	@Override
	public boolean hasBlockStateModelsForPlacementPreview(BlockState state)
	{
		return false;
	}
	
	public static void serverTick(Level level, BlockPos pos, BlockState state, GenericBlockEntity be)
	{		
		Map<NodeShape, MechanicalState> nodes = be.getData(MechanicalNodeStates.HOLDER.get());
		int existingAxleRotations = state.getValue(AXLE_ROTATION);
		// if missing mechanical data, just set axle to default state
		if (nodes == null)
		{
			if (state.getValue(AXLE_ROTATION) != 0)
			{
				level.setBlockAndUpdate(pos, state.setValue(AXLE_ROTATION, 0));
			}
			return;
		}
		Direction attachDir = state.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
		MechanicalState mechanicalState = nodes.getOrDefault(NodeShape.ofSide(attachDir), MechanicalState.ZERO);
		double radiansPerSecond = mechanicalState.angularVelocity() * attachDir.getAxisDirection().getStep();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		double seconds = gameTimeTicks * 0.05D;
		double radians = radiansPerSecond * seconds;
		double simpleRadians = radians % (Math.TAU); // gives a value in the range (-2PI, 2PI)
		if (simpleRadians < 0D)
			simpleRadians += Math.TAU;;
		// if simpleRadians > 0, we're spinning around positive axis (counterclockwise looking toward attachdir)
		int axleRotations = simpleRadians > AXLE_0_THRESHOLD ? 0	// 7/4 pi - 8/4 pi
			: simpleRadians > AXLE_3_THRESHOLD ? 3	// 5/4 pi - 7/4 pi
			: simpleRadians > AXLE_2_THRESHOLD ? 2	// 3/4 pi - 5/4 pi
			: simpleRadians > AXLE_1_THRESHOLD ? 1	// 1/4 pi - 3/4 pi
			: 0;
		if (axleRotations != existingAxleRotations)
		{
			level.setBlockAndUpdate(pos, state.setValue(AXLE_ROTATION, axleRotations));
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.ALTERNATOR_BLOCK_ENTITY.get().create(pos, state);
	}

	private static final BlockEntityTicker<GenericBlockEntity> TICKER = AlternatorBlock::serverTick;
	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return (!level.isClientSide) && type == MoreRed.ALTERNATOR_BLOCK_ENTITY.get()
			? (BlockEntityTicker<T>)TICKER
			: null;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction directionFromNeighbor)
	{
		int adjustedRotation = (state.getValue(ROTATION) + state.getValue(AXLE_ROTATION)) % 4;
		Direction directionToNeighbor = BlockStateUtil.getOutputDirection(state.getValue(ATTACHMENT_DIRECTION), adjustedRotation);
		return directionToNeighbor == directionFromNeighbor.getOpposite()
			? 15
			: 0;
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
		return side != null && side.getAxis() != state.getValue(ATTACHMENT_DIRECTION).getAxis();
	}

	// Get the redstone power output that can be conducted indirectly through solid cubes
	@Override
	public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
	{
		return blockState.getSignal(blockAccess, pos, side);
	}

	@Override
	protected void onPlace(BlockState newState, Level level, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		super.onPlace(newState, level, pos, oldState, isMoving);
		// if we are placing or rotating this block (ignoring axle rotation)
		// then we need to invoke a mechanical update here
		if (!oldState.is(this)
			|| newState.getValue(ATTACHMENT_DIRECTION) != oldState.getValue(ATTACHMENT_DIRECTION)
			|| newState.getValue(ROTATION) != oldState.getValue(ROTATION))
		{
			ExMachinaGameEvents.scheduleMechanicalGraphUpdate(level, pos);
		}
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction directionToNeighbor,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource rand)
	{
		if (level instanceof LevelAccessor levelAccess && state.getValue(ATTACHMENT_DIRECTION) == directionToNeighbor)
		{
			ExMachinaGameEvents.scheduleMechanicalGraphUpdate(levelAccess, pos);
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbor, neighborPos, neighborState, rand);
	}
	
	
}
