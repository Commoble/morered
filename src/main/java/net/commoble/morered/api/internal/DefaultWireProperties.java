package net.commoble.morered.api.internal;

import javax.annotation.Nonnull;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.commoble.morered.api.ChanneledPowerSupplier;
import net.commoble.morered.api.ExpandedPowerSupplier;
import net.commoble.morered.api.WireConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class DefaultWireProperties
{
	public static final VoxelShape[] SMALL_NODE_SHAPES = WireVoxelHelpers.makeNodeShapes(1,2);
	public static final WireConnector DEFAULT_WIRE_CONNECTOR = DefaultWireProperties::canGenericBlockConnectToWire;
	public static final ExpandedPowerSupplier DEFAULT_EXPANDED_POWER_SUPPLIER = DefaultWireProperties::getDefaultExpandedPower;
	public static final WireConnector DEFAULT_CABLE_CONNECTOR = DefaultWireProperties::canGenericBlockConnectToCable;
	public static final ChanneledPowerSupplier NO_POWER_SUPPLIER = (theWorld,pos,state,face,channel) -> 0;
	
	private static boolean canGenericBlockConnectToWire(BlockGetter world, BlockPos thisNeighborPos, BlockState thisNeighborState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		// if block is a wire or button, let the wire connect if it shares an attachment face
		// use instanceof instead of tags because base class implies block properties but tags don't
		Block neighborBlock = thisNeighborState.getBlock();
		if (neighborBlock instanceof LeverBlock || neighborBlock instanceof ButtonBlock)
		{
			AttachFace attachFace = thisNeighborState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
			return attachFace == AttachFace.FLOOR && wireFace == Direction.DOWN
				|| attachFace == AttachFace.CEILING && wireFace == Direction.UP
				|| attachFace == AttachFace.WALL && thisNeighborState.getValue(HorizontalDirectionalBlock.FACING).getOpposite() == wireFace;
		}

		if (neighborBlock instanceof RedstoneWallTorchBlock)
		{
			return wireFace == thisNeighborState.getValue(RedstoneWallTorchBlock.FACING).getOpposite();
		}
		// floor-attached blocks with unusual shapes
		else if (neighborBlock instanceof RedstoneTorchBlock
			|| neighborBlock instanceof DetectorRailBlock
			|| thisNeighborState.is(BlockTags.PRESSURE_PLATES))
		{
			return wireFace == Direction.DOWN;
		}

		if (neighborBlock instanceof LightningRodBlock)
		{
			return wireFace == thisNeighborState.getValue(LightningRodBlock.FACING).getOpposite();
		}
		
		// blocks that are almost but not quite solid
		if (thisNeighborState.is(Tags.Blocks.CHESTS_TRAPPED))
		{
			return true;
		}
		
		// we can use the tag for pressure plates since we don't need to check any properties
		if (thisNeighborState.is(BlockTags.PRESSURE_PLATES))
			return wireFace == Direction.DOWN;
		
		if (!thisNeighborState.canRedstoneConnectTo(world, wirePos, directionToWire.getOpposite()))
			return false;
		VoxelShape wireTestShape = SMALL_NODE_SHAPES[wireFace.ordinal()];
		VoxelShape neighborShape = thisNeighborState.getBlockSupportShape(world, thisNeighborPos);
		VoxelShape projectedNeighborShape = neighborShape.getFaceShape(directionToWire);
		// if the projected neighbor shape entirely overlaps the line shape,
		// then the neighbor shape can be connected to by the wire
		// we can test this by doing an ONLY_SECOND comparison on the shapes
		// if this returns true, then there are places where the second shape is not overlapped by the first
		// so if this returns false, then we can proceed
		return !Shapes.joinIsNotEmpty(projectedNeighborShape, wireTestShape, BooleanOp.ONLY_SECOND);
	}
	
	public static boolean isRedstoneWireConnectable(BlockGetter world, BlockPos redstonePos, BlockState redstoneState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		// redstone wire can connect to bottom faces of horizontally adjacent wire blocks 
		return wireFace == Direction.DOWN && directionToWire.getAxis() != Axis.Y;
	}
	
	private static int getDefaultExpandedPower(@Nonnull Level world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToThis)
	{
		return thisState.getSignal(world, thisPos, directionToThis) * 2;
	}
	
	private static boolean canGenericBlockConnectToCable(BlockGetter world, BlockPos thisPos, BlockState thisState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		return false;
	}
}
