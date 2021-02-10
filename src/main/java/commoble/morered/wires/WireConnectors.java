package commoble.morered.wires;

import commoble.morered.api.WireConnector;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

public class WireConnectors
{
	public static final WireConnector DEFAULT_WIRE_CONNECTOR = WireConnectors::canGenericBlockConnectToWire;
	
	private static boolean canGenericBlockConnectToWire(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos thisNeighborPos, BlockState thisNeighborState)
	{
		if (!thisNeighborState.canConnectRedstone(world, wirePos, directionToWire.getOpposite()))
			return false;
		VoxelShape wireTestShape = WireBlock.NODE_SHAPES_DUNSWE[wireFace.ordinal()];
		VoxelShape neighborShape = thisNeighborState.getRenderShape(world, thisNeighborPos);
		VoxelShape projectedNeighborShape = neighborShape.project(directionToWire);
		// if the projected neighbor shape entirely overlaps the line shape,
		// then the neighbor shape can be connected to by the wire
		// we can test this by doing an ONLY_SECOND comparison on the shapes
		// if this returns true, then there are places where the second shape is not overlapped by the first
		// so if this returns false, then we can proceed
		return !VoxelShapes.compare(projectedNeighborShape, wireTestShape, IBooleanFunction.ONLY_SECOND);
	}
	
	public static boolean isRedstoneWireConnectable(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		// redstone wire can connect to bottom faces of horizontally adjacent wire blocks 
		return wireFace == Direction.DOWN && directionToWire.getAxis() != Axis.Y;
	}
}
