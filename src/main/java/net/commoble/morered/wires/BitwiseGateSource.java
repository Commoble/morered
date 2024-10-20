package net.commoble.morered.wires;

import java.util.Map;
import java.util.function.ToIntFunction;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.SignalSource;
import net.commoble.morered.MoreRed;
import net.commoble.morered.ObjectNames;
import net.commoble.morered.bitwise_logic.BitewiseGateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public enum BitwiseGateSource implements SignalSource
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalSource>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_SOURCE_TYPE, MoreRed.getModRL(ObjectNames.BITWISE_GATE));
	public static final MapCodec<BitwiseGateSource> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalSource> codec()
	{
		return CODEC;
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
}
