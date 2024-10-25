package net.commoble.morered.wires;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.SignalTransmitter;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.commoble.morered.wire_post.AbstractPostBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public enum WirePostWirer implements SignalTransmitter
{
	INSTANCE;

	public static final ResourceKey<MapCodec<? extends SignalTransmitter>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_TRANSMITTER_TYPE, MoreRed.id("wire_post"));
	public static final MapCodec<WirePostWirer> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalTransmitter> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		if (!(state.getBlock() instanceof AbstractPostBlock postBlock))
			return Map.of();
		return postBlock.getTransmissionNodes(level, pos, state, face);
	}
}
