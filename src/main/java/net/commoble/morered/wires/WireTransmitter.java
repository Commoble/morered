package net.commoble.morered.wires;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.SignalTransmitter;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public enum WireTransmitter implements SignalTransmitter
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalTransmitter>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_TRANSMITTER_TYPE, MoreRed.id("wire"));
	public static final MapCodec<WireTransmitter> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalTransmitter> codec()
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
}
