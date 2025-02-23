package net.commoble.morered.bitwise_logic;

import java.util.Collection;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.commoble.morered.Names;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public enum BitwiseGateSignalComponent implements SignalComponent
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalComponent>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_COMPONENT_TYPE, MoreRed.id(Names.BITWISE_GATE));
	public static final MapCodec<BitwiseGateSignalComponent> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalComponent> codec()
	{
		return CODEC;
	}

	@Override
	public Collection<TransmissionNode> getTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel)
	{
		return level.getBlockEntity(pos) instanceof BitwiseGateBlockEntity gate
			? gate.getTransmissionNodes(levelKey, level, pos, state, channel)
			: List.of();
	}

}
