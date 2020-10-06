package commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import commoble.morered.plate_blocks.PlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

public class WirePostPlateBlock extends AbstractWirePostBlock
{
	protected static final VoxelShape[] PLATED_POST_SHAPES_DUNSWE = {
		VoxelShapes.or(AbstractWirePostBlock.POST_SHAPES_DUNSWE[0], PlateBlock.SHAPES_BY_DIRECTION[0]), // down
		VoxelShapes.or(AbstractWirePostBlock.POST_SHAPES_DUNSWE[1], PlateBlock.SHAPES_BY_DIRECTION[1]), // up
		VoxelShapes.or(AbstractWirePostBlock.POST_SHAPES_DUNSWE[2], PlateBlock.SHAPES_BY_DIRECTION[2]), // north
		VoxelShapes.or(AbstractWirePostBlock.POST_SHAPES_DUNSWE[3], PlateBlock.SHAPES_BY_DIRECTION[3]), // south
		VoxelShapes.or(AbstractWirePostBlock.POST_SHAPES_DUNSWE[4], PlateBlock.SHAPES_BY_DIRECTION[4]), // west
		VoxelShapes.or(AbstractWirePostBlock.POST_SHAPES_DUNSWE[5], PlateBlock.SHAPES_BY_DIRECTION[5]) // east
	};

	public WirePostPlateBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties, connectionGetter);
	}
	
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		// if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
		VoxelShape[] shapeTable = context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext)context).shouldIgnoreBlock(pos)
			? PlateBlock.SHAPES_BY_DIRECTION
			: PLATED_POST_SHAPES_DUNSWE;
		return shapeTable[state.hasProperty(DIRECTION_OF_ATTACHMENT) ? state.get(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
	}
	
	public static EnumSet<Direction> getRedstoneConnectionDirectionsForEmptyPlate(BlockState state)
	{
		return AbstractWirePostBlock.NO_DIRECTIONS;
	}
	
	public static EnumSet<Direction> getRedstoneConnectionDirectionsForRelayPlate(BlockState state)
	{
		if (!state.hasProperty(DIRECTION_OF_ATTACHMENT))
		{
			return AbstractWirePostBlock.NO_DIRECTIONS;
		}
		
		Direction attachmentDir = state.get(DIRECTION_OF_ATTACHMENT);
		return EnumSet.complementOf(EnumSet.of(attachmentDir, attachmentDir.getOpposite()));
	}

}
