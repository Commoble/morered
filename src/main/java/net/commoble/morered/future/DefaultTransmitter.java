package net.commoble.morered.future;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public enum DefaultTransmitter implements SignalTransmitter
{
	INSTANCE;

	public static final ResourceKey<MapCodec<? extends SignalTransmitter>> RESOURCE_KEY = ResourceKey.create(SignalTransmitter.REGISTRY_KEY, MoreRed.getModRL("default"));
	public static final MapCodec<DefaultTransmitter> CODEC = MapCodec.unit(INSTANCE);
	
	@Override
	public MapCodec<? extends SignalTransmitter> codec()
	{
		return CODEC;
	}
	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		return Map.of();
	}
}
