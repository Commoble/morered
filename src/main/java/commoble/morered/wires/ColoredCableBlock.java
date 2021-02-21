package commoble.morered.wires;

import com.google.common.cache.LoadingCache;

import commoble.morered.api.MoreRedAPI;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.DyeColor;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

public class ColoredCableBlock extends PoweredWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = AbstractWireBlock.makeNodeShapes(2, 3);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = AbstractWireBlock.makeRaytraceBackboards(3);
	public static final VoxelShape[] LINE_SHAPES = AbstractWireBlock.makeLineShapes(2, 3);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);
	
	private final DyeColor color; public DyeColor getDyeColor() { return this.color; }

	public ColoredCableBlock(Properties properties, DyeColor color)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false);
		this.color = color;
	}
	
	public boolean canConnectToAdjacentWireOrCable(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos neighborPos,
		BlockState neighborState)
	{
		Block wireBlock = wireState.getBlock();
		return wireBlock instanceof ColoredCableBlock && ((ColoredCableBlock)wireBlock).getDyeColor() != this.getDyeColor()
			? false
			: AbstractWireBlock.canWireConnectToAdjacentWireOrCable(world, wirePos, wireState, wireFace, directionToWire, neighborPos, neighborState);
	}

	@Override
	protected boolean canAdjacentBlockConnectToFace(IBlockReader world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		// do this check first so we don't check the same behaviour twice for colored cable blocks
		return neighborBlock instanceof ColoredCableBlock
			? ((ColoredCableBlock)neighborBlock).canConnectToAdjacentWireOrCable(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState)
			: MoreRedAPI.getWireConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultWireConnector())
				.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState)
				||
				MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
				.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState);
	}
	
}
