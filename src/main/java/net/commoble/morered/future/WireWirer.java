package net.commoble.morered.future;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.wires.WireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public enum WireWirer implements Wirer
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends Wirer>> RESOURCE_KEY = ResourceKey.create(Wirer.REGISTRY_KEY, MoreRed.getModRL("wire"));
	public static final MapCodec<WireWirer> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends Wirer> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		if (!(level.getBlockEntity(pos) instanceof WireBlockEntity wire))
			return Map.of();
		
		return wire.getTransmissionNodes(face, state);
	}

	@Override
	public Map<Channel, Function<LevelReader,Integer>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace)
	{
		return Map.of();
	}

	@Override
	public Map<Channel,BiConsumer<LevelAccessor,Integer>> getReceiverEndpoints(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace)
	{
		return Map.of();
	}

	@Override
	public boolean ignoreVanillaSignal(LevelReader reader, BlockPos wirerPos, BlockState wirerState, Direction directionFromNeighbor)
	{
		return true;
	}
}
