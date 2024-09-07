package net.commoble.morered.future;

import java.util.Map;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.internal.DefaultWireProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum DefaultWirer implements Wirer
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends Wirer>> RESOURCE_KEY = ResourceKey.create(Wirer.REGISTRY_KEY, MoreRed.getModRL("default"));
	public static final MapCodec<DefaultWirer> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends Wirer> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(LevelReader level, BlockPos pos, BlockState state, Direction face)
	{
		return Map.of();
	}

	@Override
	public Map<Channel, Integer> getSupplierEndpoints(LevelReader level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace)
	{
		// we allow wires to connect to vanilla power emitters by default if the block is redstone-connectable and has a connectable voxelshape
		BlockPos wirePos = connectedFace.pos();
		BlockPos offsetFromNeighbor = supplierPos.subtract(wirePos);
		@Nullable Direction directionFromNeighbor = Direction.fromDelta(offsetFromNeighbor.getX(), offsetFromNeighbor.getY(), offsetFromNeighbor.getZ()); 
		if (!supplierState.canRedstoneConnectTo(level, wirePos, directionFromNeighbor))
			return Map.of();
		Direction directionToWire = directionFromNeighbor.getOpposite();
		VoxelShape wireTestShape = DefaultWireProperties.SMALL_NODE_SHAPES[connectedFace.attachmentSide().ordinal()];
		VoxelShape neighborShape = supplierState.getBlockSupportShape(level, supplierPos);
		VoxelShape projectedNeighborShape = neighborShape.getFaceShape(directionToWire);
		// if the projected neighbor shape entirely overlaps the line shape,
		// then the neighbor shape can be connected to by the wire
		// we can test this by doing an ONLY_SECOND comparison on the shapes
		// if this returns true, then there are places where the second shape is not overlapped by the first
		// so if this returns false, then we can proceed
		return Shapes.joinIsNotEmpty(projectedNeighborShape, wireTestShape, BooleanOp.ONLY_SECOND)
			? Map.of()
			: Map.of(Channel.wide(), level.getSignal(supplierPos, directionFromNeighbor));
	}

	@Override
	public Map<Channel, IntConsumer> getReceiverEndpoints(LevelReader level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace)
	{
		return Map.of();
	}
}
