package net.commoble.morered.wire_post;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableJunctionBlock extends AbstractBundledCablePostBlock
{
	protected static final VoxelShape[] CABLE_PLATE_SHAPES_DUNSWE = {
		Block.box(0D,0D,0D,16D,4D,16D),
		Block.box(0D,12D,0D,16D,16D,16D),
		Block.box(0D,0D,0D,16D,16D,4D),
		Block.box(0D,0D,12D,16D,16D,16D),
		Block.box(0D,0D,0D,4D,16D,16D),
		Block.box(12D,0D,0D,16D,16D,16D)
	};
	
	protected static final VoxelShape[] PLATED_CABLE_POST_SHAPES_DUNSWE = {
		Shapes.or(AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[0], CABLE_PLATE_SHAPES_DUNSWE[0]), // down
		Shapes.or(AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[1], CABLE_PLATE_SHAPES_DUNSWE[1]), // up
		Shapes.or(AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[2], CABLE_PLATE_SHAPES_DUNSWE[2]), // north
		Shapes.or(AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[3], CABLE_PLATE_SHAPES_DUNSWE[3]), // south
		Shapes.or(AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[4], CABLE_PLATE_SHAPES_DUNSWE[4]), // west
		Shapes.or(AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[5], CABLE_PLATE_SHAPES_DUNSWE[5]) // east
	};

	public CableJunctionBlock(Properties properties)
	{
		super(properties, WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate);
	}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		// if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
		VoxelShape[] shapeTable = context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext)context).shouldIgnoreBlock(pos)
			? CABLE_PLATE_SHAPES_DUNSWE
			: PLATED_CABLE_POST_SHAPES_DUNSWE;
		return shapeTable[state.hasProperty(DIRECTION_OF_ATTACHMENT) ? state.getValue(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
	}
}
