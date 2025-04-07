package net.commoble.morered.mechanisms;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.MoreRed;
import net.commoble.morered.PlayerData;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

public class GearshifterBlock extends Block implements EntityBlock, SimpleWaterloggedBlock
{
	// direction of big gear attachment
	public static final EnumProperty<Direction> ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	// rotation of small gear about the axis through the big gear
	// when attachment is DOWN and rotation is 0, small gear points NORTH
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	
	public static final Direction DEFAULT_BIG_DIR = Direction.DOWN;
	public static final Direction DEFAULT_SMALL_DIR = Direction.NORTH;
	public static final Direction DEFAULT_AXLE_DIR = DEFAULT_SMALL_DIR.getOpposite();

	public GearshifterBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(ATTACHMENT_DIRECTION, DEFAULT_BIG_DIR)
			.setValue(ROTATION, 0)
			.setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(ATTACHMENT_DIRECTION, ROTATION, WATERLOGGED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().gearshifterBlockEntity.get().create(pos, state);
	}
	
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		// we should probably rename this since it's not exactly just plate blocks anymore, it's 24-orientation blocks
		return PlateBlockStateProperties.getStateForPlacedGatePlate(this.defaultBlockState(), context);
	}
	
	public static Map<NodeShape,MechanicalState> normalizeMachine(BlockState state, HolderLookup.Provider provider, Map<NodeShape,MechanicalState> runtimeData)
	{
		// let the default state point to down and north, for the big gear and little gear's nodes, respectively
		// so, no matter which way we're pointing in-world,
		// store those two states in those two directions
		Map<NodeShape,MechanicalState> result = new HashMap<>();
		
		// big gear is easier, we can get that directly from the attachment property
		Direction bigDir = state.getValue(ATTACHMENT_DIRECTION);
		MechanicalState bigGearState = runtimeData.getOrDefault(NodeShape.ofSide(bigDir), MechanicalState.ZERO);
		result.put(NodeShape.ofSide(DEFAULT_BIG_DIR), bigGearState);
		
		// small gear is trickier, but we can reuse the plate block utils for that
		// let small gear = "output direction"
		int rotation = state.getValue(ROTATION);
		Direction smallDir = BlockStateUtil.getOutputDirection(bigDir, rotation);
		result.put(NodeShape.ofSide(DEFAULT_SMALL_DIR), runtimeData.getOrDefault(NodeShape.ofSide(smallDir), MechanicalState.ZERO));
		// there's also an unused node in the opposite direction of the small gear, representing the axle
		// better add the "opposite node" data just in case we need it later
		Direction axleDir = smallDir.getOpposite();
		result.put(NodeShape.ofSide(DEFAULT_AXLE_DIR), runtimeData.getOrDefault(NodeShape.ofSide(axleDir), MechanicalState.ZERO));
		return result;
	}
	
	public static Map<NodeShape,MechanicalState> denormalizeMachine(BlockState state, HolderLookup.Provider provider, Map<NodeShape,MechanicalState> diskData)
	{
		Map<NodeShape,MechanicalState> result = new HashMap<>();
		MechanicalState bigState = diskData.getOrDefault(NodeShape.ofSide(DEFAULT_BIG_DIR), MechanicalState.ZERO);
		MechanicalState smallState = diskData.getOrDefault(NodeShape.ofSide(DEFAULT_SMALL_DIR), MechanicalState.ZERO);
		MechanicalState axleState = diskData.getOrDefault(NodeShape.ofSide(DEFAULT_AXLE_DIR), MechanicalState.ZERO);
		Direction bigDir = state.getValue(ATTACHMENT_DIRECTION);
		Direction smallDir = BlockStateUtil.getOutputDirection(bigDir, state.getValue(ROTATION));
		Direction axleDir = smallDir.getOpposite();
		result.put(NodeShape.ofSide(bigDir), bigState);
		result.put(NodeShape.ofSide(smallDir), smallState);
		result.put(NodeShape.ofSide(axleDir), axleState);
		return result;
	}

	@Override
	protected BlockState updateShape(BlockState thisState, LevelReader level, ScheduledTickAccess ticks, BlockPos thisPos, Direction directionToNeighbor, BlockPos neighborPos,
		BlockState neighborState, RandomSource rand)
	{
		if (thisState.getValue(WATERLOGGED))
		{
			ticks.scheduleTick(thisPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return super.updateShape(thisState, level, ticks, thisPos, directionToNeighbor, neighborPos, neighborState, rand);
	}
	
	@Override
	protected FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	
	@Override
	public BlockState rotate(BlockState state, Rotation rotation)
	{
		if (state.hasProperty(ATTACHMENT_DIRECTION) && state.hasProperty(ROTATION))
		{
			Direction attachmentDirection = state.getValue(ATTACHMENT_DIRECTION);
			int rotationIndex = state.getValue(ROTATION);

			Direction newAttachmentDirection = rotation.rotate(attachmentDirection);
			int newRotationIndex = BlockStateUtil.getRotatedRotation(attachmentDirection, rotationIndex, rotation);

			return state.setValue(ATTACHMENT_DIRECTION, newAttachmentDirection).setValue(ROTATION, newRotationIndex);
		}
		else
		{
			return state;
		}
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		if (state.hasProperty(ATTACHMENT_DIRECTION) && state.hasProperty(ROTATION))
		{
			Direction attachmentDirection = state.getValue(ATTACHMENT_DIRECTION);
			int rotationIndex = state.getValue(ROTATION);

			Direction newAttachmentDirection = mirror.mirror(attachmentDirection);
			int newRotationIndex = BlockStateUtil.getMirroredRotation(attachmentDirection, rotationIndex, mirror);

			return state.setValue(ATTACHMENT_DIRECTION, newAttachmentDirection).setValue(ROTATION, newRotationIndex);
		}
		else
		{
			return state;
		}
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return Shapes.empty();
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
	{
		return 1.0F;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state)
	{
		return true;
	}
	
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		boolean isPlayerHoldingWrench = stack.is(Tags.Items.TOOLS_WRENCH);
		
		// rotate the block when the player pokes it with a wrench
		if (isPlayerHoldingWrench && !level.isClientSide)
		{
			BlockState newState;
			level.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS,
				0.9F + level.random.nextFloat()*0.1F,
				0.95F + level.random.nextFloat()*0.1F);
			if (PlayerData.getSprinting(player.getUUID()))
			{
				// rotate around small gear... weird math here
				// firstly, figure out which way small gear is facing
				Direction bigDir = state.getValue(ATTACHMENT_DIRECTION);
				int rotation = state.getValue(ROTATION);
				Direction smallDir = BlockStateUtil.getOutputDirection(bigDir, rotation);
				// now we'd like to rotate bigDir around the smalldir axis
				// use our rotation indexer using smallDir as the primary axis
				int bigRotationIndex = BlockStateUtil.getRotationIndexForDirection(smallDir, bigDir);
				int nextBigRotationIndex = (bigRotationIndex+1) % 4;
				Direction newBigDir = BlockStateUtil.getOutputDirection(smallDir, nextBigRotationIndex);
				// now we just need to find the rotation index that preserves smalldir
				int newSmallRotation = BlockStateUtil.getRotationIndexForDirection(newBigDir, smallDir);
				newState = state.setValue(ATTACHMENT_DIRECTION, newBigDir)
					.setValue(ROTATION, newSmallRotation);
			}
			else
			{
				// rotate around big gear
				int newRotation = (state.getValue(ROTATION) + 1) % 4;
				newState = state.setValue(ROTATION, newRotation);
			}
			level.setBlockAndUpdate(pos, newState);
		}
		
		return isPlayerHoldingWrench ? InteractionResult.SUCCESS : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}
}
