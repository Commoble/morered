package net.commoble.morered.api;

import java.util.Map;
import java.util.function.ToIntFunction;

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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public interface SignalSource
{
	public static final ResourceKey<Registry<MapCodec<? extends SignalSource>>> REGISTRY_KEY = ResourceKey.createRegistryKey(MoreRed.getModRL("signal_source"));
	public static final Codec<SignalSource> CODEC = MoreCodecs.dispatch(REGISTRY_KEY, SignalSource::codec);
	public static final DataMapType<Block, SignalSource> DATA_MAP_TYPE = DataMapType.builder(MoreRed.getModRL("signal_source"), Registries.BLOCK, CODEC).build();

	public abstract MapCodec<? extends SignalSource> codec();
	public abstract Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace);
}
