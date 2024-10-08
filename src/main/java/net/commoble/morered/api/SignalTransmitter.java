package net.commoble.morered.api;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.util.MoreCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public interface SignalTransmitter
{
	public static final ResourceKey<Registry<MapCodec<? extends SignalTransmitter>>> REGISTRY_KEY = ResourceKey.createRegistryKey(MoreRed.getModRL("wirer"));
	public static final Codec<SignalTransmitter> CODEC = MoreCodecs.dispatch(REGISTRY_KEY, SignalTransmitter::codec);
	public static final DataMapType<Block, SignalTransmitter> DATA_MAP_TYPE = DataMapType.builder(MoreRed.getModRL("signal_transmitter"), Registries.BLOCK, CODEC).build();

	public abstract MapCodec<? extends SignalTransmitter> codec();
	public abstract Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face);
}
