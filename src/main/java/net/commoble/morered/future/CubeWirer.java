package net.commoble.morered.future;

import java.util.Map;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public enum CubeWirer implements Wirer
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends Wirer>> RESOURCE_KEY = ResourceKey.create(Wirer.REGISTRY_KEY, MoreRed.getModRL("cube"));
	public static final MapCodec<CubeWirer> CODEC = MapCodec.unit(INSTANCE);

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
		BlockPos offsetFromNeighbor = supplierPos.subtract(connectedFace.pos());
		@Nullable Direction directionFromNeighbor = Direction.fromDelta(offsetFromNeighbor.getX(), offsetFromNeighbor.getY(), offsetFromNeighbor.getZ()); 
		return directionFromNeighbor == null
			? Map.of()
			: Map.of(Channel.wide(), level.getSignal(supplierPos, directionFromNeighbor));
	}

	@Override
	public Map<Channel, IntConsumer> getReceiverEndpoints(LevelReader level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace)
	{
		return Map.of();
	}

}
