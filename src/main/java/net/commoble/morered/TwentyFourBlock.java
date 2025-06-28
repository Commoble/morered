package net.commoble.morered;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Block which can be attached to any of six faces, and then rotated in four orientations about the axis of the attachment direction.
 * We define these terms:
 * - the Primary Direction is the direction to the neighbor the block is attached to (default DOWN)
 * - the Secondary or Output Direction is, when not rotated, NORTH if the attachment direction is vertical, or UP if the attachment direction is horizontal
 * - increasing rotation rotates the secondary direction positively about the primary direction's axis
 */
public class TwentyFourBlock extends Block
{
	public static final EnumProperty<Direction> ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;
	public static final Direction DEFAULT_ATTACH_DIR = Direction.DOWN;

	public TwentyFourBlock(Properties props)
	{
		super(props);
		
		this.registerDefaultState(this.defaultBlockState()
			.setValue(ATTACHMENT_DIRECTION, DEFAULT_ATTACH_DIR)
			.setValue(ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(ATTACHMENT_DIRECTION, ROTATION);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		// we should probably rename this since it's not exactly just plate blocks anymore, it's 24-orientation blocks
		return PlateBlockStateProperties.getStateForPlacedGatePlate(this.defaultBlockState(), context);
	}
	
	@Override
	@Deprecated
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
	@Deprecated
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
	
	/**
	 * If true, placement preview will just render the blockstate.
	 * If false, we'll manually rotate the matrix based on state, and then render the item model
	 * @param state
	 * @return true if we should render the blockstate, false if we should render the item
	 */
	public boolean hasBlockStateModelsForPlacementPreview(BlockState state)
	{
		return this.getRenderShape(state) == RenderShape.MODEL;
	}
	
	public static Map<NodeShape,MechanicalState> normalizeMachineWithAttachmentNode(BlockState state, Map<NodeShape,MechanicalState> runtimeData)
	{
		Map<NodeShape,MechanicalState> result = new HashMap<>();
	
		Direction attachDir = state.getValue(ATTACHMENT_DIRECTION);
		MechanicalState attachState = runtimeData.getOrDefault(NodeShape.ofSide(attachDir), MechanicalState.ZERO);
		result.put(NodeShape.ofSide(DEFAULT_ATTACH_DIR), attachState);
		return result;
	}
	
	public static Map<NodeShape,MechanicalState> denormalizeMachineWithAttachmentNode(BlockState state, Map<NodeShape,MechanicalState> diskData)
	{
		Map<NodeShape,MechanicalState> result = new HashMap<>();
		MechanicalState attachState = diskData.getOrDefault(NodeShape.ofSide(DEFAULT_ATTACH_DIR), MechanicalState.ZERO);
		Direction attachDir = state.getValue(ATTACHMENT_DIRECTION);
		result.put(NodeShape.ofSide(attachDir), attachState);
		return result;
	}
}
