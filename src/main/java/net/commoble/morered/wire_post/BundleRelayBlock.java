package net.commoble.morered.wire_post;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BundleRelayBlock extends AbstractBundledCablePostBlock
{

	public BundleRelayBlock(Properties properties)
	{
		super(properties, WirePostBlock::noParallelConnections);
	}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		// if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
		return context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext)context).shouldIgnoreBlock(pos)
			? Shapes.empty()
			: AbstractBundledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[state.hasProperty(DIRECTION_OF_ATTACHMENT) ? state.getValue(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
	}
}
