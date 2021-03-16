package commoble.morered.plate_blocks;

import javax.annotation.Nullable;

import commoble.morered.util.BlockStateUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

public class PlateBlock extends Block
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = PlateBlockStateProperties.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = PlateBlockStateProperties.ROTATION;

	public static final VoxelShape[] SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 14, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 2),
		Block.box(0, 0, 14, 16, 16, 16), Block.box(0, 0, 0, 2, 16, 16), Block.box(14, 0, 0, 16, 16, 16) };
	

	public PlateBlock(Properties properties)
	{
		super(properties);
		
		BlockState baseState = this.getStateDefinition().any()
			.setValue(ATTACHMENT_DIRECTION, Direction.DOWN)
			.setValue(ROTATION, 0);
		
		this.registerDefaultState(baseState);
	}

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(ATTACHMENT_DIRECTION, ROTATION);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		return PlateBlockStateProperties.getStateForPlacedGatePlate(this.defaultBlockState(), context);
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		if (state.hasProperty(ATTACHMENT_DIRECTION))
		{
			return SHAPES_BY_DIRECTION[state.getValue(ATTACHMENT_DIRECTION).ordinal()];
		}
		else
		{
			return SHAPES_BY_DIRECTION[0];
		}
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
}
