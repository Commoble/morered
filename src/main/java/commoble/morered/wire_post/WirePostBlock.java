package commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

import net.minecraft.block.AbstractBlock.Properties;

public class WirePostBlock extends AbstractPoweredWirePostBlock
{

	public WirePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties, connectionGetter);
	}
	
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		// if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
		return context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext)context).shouldIgnoreBlock(pos)
			? VoxelShapes.empty()
			: AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[state.hasProperty(DIRECTION_OF_ATTACHMENT) ? state.get(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
	}
	
	public static EnumSet<Direction> getRedstoneConnectionDirections(BlockState state)
	{
		return state.hasProperty(DIRECTION_OF_ATTACHMENT)
			? EnumSet.of(state.get(DIRECTION_OF_ATTACHMENT))
			: AbstractPoweredWirePostBlock.NO_DIRECTIONS;
	}

}
