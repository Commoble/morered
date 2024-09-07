package net.commoble.morered.future;

import java.util.Map;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.util.MoreCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface Wirer
{
	public static final ResourceKey<Registry<MapCodec<? extends Wirer>>> REGISTRY_KEY = ResourceKey.createRegistryKey(MoreRed.getModRL("wirer"));
	public static final Codec<Wirer> CODEC = MoreCodecs.dispatch(REGISTRY_KEY, Wirer::codec);

	public abstract MapCodec<? extends Wirer> codec();
	public abstract Map<Channel, TransmissionNode> getTransmissionNodes(LevelReader level, BlockPos pos, BlockState state, Direction face);
	public abstract Map<Channel, Integer> getSupplierEndpoints(LevelReader level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace);
	public abstract Map<Channel, IntConsumer> getReceiverEndpoints(LevelReader level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace);
	public default boolean ignoreVanillaSignal(LevelReader reader, BlockPos wirerPos, BlockState wirerState, Direction directionFromNeighbor)
	{
		return false;
	}
}
