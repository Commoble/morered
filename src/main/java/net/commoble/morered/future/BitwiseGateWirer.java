package net.commoble.morered.future;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.ObjectNames;
import net.commoble.morered.bitwise_logic.BitewiseGateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public enum BitwiseGateWirer implements Wirer
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends Wirer>> RESOURCE_KEY = ResourceKey.create(Wirer.REGISTRY_KEY, MoreRed.getModRL(ObjectNames.BITWISE_GATE));
	public static final MapCodec<BitwiseGateWirer> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends Wirer> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		return Map.of();
	}

	@Override
	public Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide,
		Face connectedFace)
	{
		if (supplierState.getBlock() instanceof BitewiseGateBlock block)
		{
			return block.getSupplierEndpoints(level, supplierPos, supplierState, supplierSide, connectedFace);
		}
		return Map.of();
	}

	@Override
	public Map<Channel, BiConsumer<LevelAccessor, Integer>> getReceiverEndpoints(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide,
		Face connectedFace)
	{
		if (receiverState.getBlock() instanceof BitewiseGateBlock block)
		{
			return block.getReceiverEndpoints(level, receiverPos, receiverState, receiverSide, connectedFace);
		}
		return Map.of();
	}

}
