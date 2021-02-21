package commoble.morered.wires;

import javax.annotation.Nonnull;

import commoble.morered.api.ExpandedPowerSupplier;
import commoble.morered.api.WireConnector;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class DefaultWireProperties
{
	public static final WireConnector DEFAULT_WIRE_CONNECTOR = DefaultWireProperties::canGenericBlockConnectToWire;
	public static final ExpandedPowerSupplier DEFAULT_EXPANDED_POWER_SUPPLIER = DefaultWireProperties::getDefaultExpandedPower;
	public static final WireConnector DEFAULT_CABLE_CONNECTOR = DefaultWireProperties::canGenericBlockConnectToCable;
	
	private static boolean canGenericBlockConnectToWire(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos thisNeighborPos, BlockState thisNeighborState)
	{
		if (!thisNeighborState.canConnectRedstone(world, wirePos, directionToWire.getOpposite()))
			return false;
		VoxelShape wireTestShape = RedAlloyWireBlock.NODE_SHAPES_DUNSWE[wireFace.ordinal()];
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
	
	public static int getDefaultExpandedPower(@Nonnull World world, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToThis, @Nonnull BlockPos thisNeighborPos, @Nonnull BlockState thisNeighborState)
	{
		return thisNeighborState.getWeakPower(world, thisNeighborPos, directionToThis) * 2;
	}
	
	public static boolean canGenericBlockConnectToCable(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos thisNeighborPos, BlockState thisNeighborState)
	{
		return false;
	}
}
