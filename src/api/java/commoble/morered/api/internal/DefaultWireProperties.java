package commoble.morered.api.internal;

import javax.annotation.Nonnull;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.ExpandedPowerSupplier;
import commoble.morered.api.WireConnector;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.HorizontalFaceBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.tags.BlockTags;
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
	public static final VoxelShape[] SMALL_NODE_SHAPES = WireVoxelHelpers.makeNodeShapes(1,2);
	public static final WireConnector DEFAULT_WIRE_CONNECTOR = DefaultWireProperties::canGenericBlockConnectToWire;
	public static final ExpandedPowerSupplier DEFAULT_EXPANDED_POWER_SUPPLIER = DefaultWireProperties::getDefaultExpandedPower;
	public static final WireConnector DEFAULT_CABLE_CONNECTOR = DefaultWireProperties::canGenericBlockConnectToCable;
	public static final ChanneledPowerSupplier NO_POWER_SUPPLIER = (theWorld,pos,state,face,channel) -> 0;
	
	private static boolean canGenericBlockConnectToWire(IBlockReader world, BlockPos thisNeighborPos, BlockState thisNeighborState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		// if block is a wire or button, let the wire connect if it shares an attachment face
		// use instanceof instead of tags because base class implies block properties but tags don't
		Block neighborBlock = thisNeighborState.getBlock();
		if (neighborBlock instanceof LeverBlock || neighborBlock instanceof AbstractButtonBlock)
		{
			AttachFace attachFace = thisNeighborState.getValue(HorizontalFaceBlock.FACE);
			return attachFace == AttachFace.FLOOR && wireFace == Direction.DOWN
				|| attachFace == AttachFace.CEILING && wireFace == Direction.UP
				|| attachFace == AttachFace.WALL && thisNeighborState.getValue(HorizontalBlock.FACING).getOpposite() == wireFace;
		}
		
		// we can use the tag for pressure plates since we don't need to check any properties
		if (thisNeighborState.is(BlockTags.PRESSURE_PLATES))
			return wireFace == Direction.DOWN;
		
		if (!thisNeighborState.canConnectRedstone(world, wirePos, directionToWire.getOpposite()))
			return false;
		VoxelShape wireTestShape = SMALL_NODE_SHAPES[wireFace.ordinal()];
		VoxelShape neighborShape = thisNeighborState.getBlockSupportShape(world, thisNeighborPos);
		VoxelShape projectedNeighborShape = neighborShape.getFaceShape(directionToWire);
		// if the projected neighbor shape entirely overlaps the line shape,
		// then the neighbor shape can be connected to by the wire
		// we can test this by doing an ONLY_SECOND comparison on the shapes
		// if this returns true, then there are places where the second shape is not overlapped by the first
		// so if this returns false, then we can proceed
		return !VoxelShapes.joinIsNotEmpty(projectedNeighborShape, wireTestShape, IBooleanFunction.ONLY_SECOND);
	}
	
	public static boolean isRedstoneWireConnectable(IBlockReader world, BlockPos redstonePos, BlockState redstoneState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		// redstone wire can connect to bottom faces of horizontally adjacent wire blocks 
		return wireFace == Direction.DOWN && directionToWire.getAxis() != Axis.Y;
	}
	
	private static int getDefaultExpandedPower(@Nonnull World world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToThis)
	{
		return thisState.getSignal(world, thisPos, directionToThis) * 2;
	}
	
	private static boolean canGenericBlockConnectToCable(IBlockReader world, BlockPos thisPos, BlockState thisState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		return false;
	}
}
