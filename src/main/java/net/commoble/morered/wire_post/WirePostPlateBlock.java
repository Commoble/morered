package net.commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import net.commoble.morered.plate_blocks.PlateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WirePostPlateBlock extends AbstractPoweredWirePostBlock
{
	protected static final VoxelShape[] PLATED_POST_SHAPES_DUNSWE = {
		Shapes.or(AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[0], PlateBlock.SHAPES_BY_DIRECTION[0]), // down
		Shapes.or(AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[1], PlateBlock.SHAPES_BY_DIRECTION[1]), // up
		Shapes.or(AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[2], PlateBlock.SHAPES_BY_DIRECTION[2]), // north
		Shapes.or(AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[3], PlateBlock.SHAPES_BY_DIRECTION[3]), // south
		Shapes.or(AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[4], PlateBlock.SHAPES_BY_DIRECTION[4]), // west
		Shapes.or(AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[5], PlateBlock.SHAPES_BY_DIRECTION[5]) // east
	};

	public WirePostPlateBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties, connectionGetter);
	}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		// if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
		VoxelShape[] shapeTable = context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext)context).shouldIgnoreBlock(pos)
			? PlateBlock.SHAPES_BY_DIRECTION
			: PLATED_POST_SHAPES_DUNSWE;
		return shapeTable[state.hasProperty(DIRECTION_OF_ATTACHMENT) ? state.getValue(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
	}
	
	public static EnumSet<Direction> getRedstoneConnectionDirectionsForEmptyPlate(BlockState state)
	{
		return AbstractPoweredWirePostBlock.NO_DIRECTIONS;
	}
	
	public static EnumSet<Direction> getRedstoneConnectionDirectionsForRelayPlate(BlockState state)
	{
		if (!state.hasProperty(DIRECTION_OF_ATTACHMENT))
		{
			return AbstractPoweredWirePostBlock.NO_DIRECTIONS;
		}
		
		Direction attachmentDir = state.getValue(DIRECTION_OF_ATTACHMENT);
		return EnumSet.complementOf(EnumSet.of(attachmentDir, attachmentDir.getOpposite()));
	}

}
