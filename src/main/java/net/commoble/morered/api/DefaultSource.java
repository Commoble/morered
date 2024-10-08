package net.commoble.morered.api;

import java.util.Map;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum DefaultSource implements SignalSource
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalSource>> RESOURCE_KEY = ResourceKey.create(SignalSource.REGISTRY_KEY, MoreRed.getModRL("default"));
	public static final MapCodec<DefaultSource> CODEC = MapCodec.unit(INSTANCE);

	private static final VoxelShape[] SMALL_NODE_SHAPES = WireVoxelHelpers.makeNodeShapes(1,2);

	@Override
	public MapCodec<? extends SignalSource> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace)
	{
		// we allow wires to connect to vanilla power emitters by default if the block is redstone-connectable and has a connectable voxelshape
		BlockPos wirePos = connectedFace.pos();
		BlockPos offsetFromNeighbor = supplierPos.subtract(wirePos);
		@Nullable Direction directionFromNeighbor = Direction.fromDelta(offsetFromNeighbor.getX(), offsetFromNeighbor.getY(), offsetFromNeighbor.getZ()); 
		if (!supplierState.canRedstoneConnectTo(level, wirePos, directionFromNeighbor))
			return Map.of();
		Direction directionToWire = directionFromNeighbor.getOpposite();
		VoxelShape wireTestShape = SMALL_NODE_SHAPES[connectedFace.attachmentSide().ordinal()];
		VoxelShape neighborShape = supplierState.getBlockSupportShape(level, supplierPos);
		VoxelShape projectedNeighborShape = neighborShape.getFaceShape(directionToWire);
		// if the projected neighbor shape entirely overlaps the line shape,
		// then the neighbor shape can be connected to by the wire
		// we can test this by doing an ONLY_SECOND comparison on the shapes
		// if this returns true, then there are places where the second shape is not overlapped by the first
		// so if this returns false, then we can proceed
		return Shapes.joinIsNotEmpty(projectedNeighborShape, wireTestShape, BooleanOp.ONLY_SECOND)
			? Map.of()
			: Map.of(Channel.wide(), reader -> reader.getSignal(supplierPos, directionFromNeighbor));
	}
}
