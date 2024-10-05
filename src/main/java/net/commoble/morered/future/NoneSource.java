package net.commoble.morered.future;

import java.util.Map;
import java.util.function.ToIntFunction;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public enum NoneSource implements SignalSource
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalSource>> RESOURCE_KEY = ResourceKey.create(SignalSource.REGISTRY_KEY, MoreRed.getModRL("none"));
	public static final MapCodec<NoneSource> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalSource> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide,
		Face connectedFace)
	{
		return Map.of();
	}
}
