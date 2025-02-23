package net.commoble.morered.wires;

import java.util.Collection;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public enum WireSignalComponent implements SignalComponent
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalComponent>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_COMPONENT_TYPE, MoreRed.id("wire"));
	public static final MapCodec<WireSignalComponent> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalComponent> codec()
	{
		return CODEC;
	}

	@Override
	public Collection<TransmissionNode> getTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel)
	{
		if (!(level.getBlockEntity(pos) instanceof WireBlockEntity wire))
			return List.of();
		
		return wire.getTransmissionNodes(levelKey, level, state, channel);
	}
}
