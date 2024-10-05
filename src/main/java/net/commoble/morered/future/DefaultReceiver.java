package net.commoble.morered.future;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public enum DefaultReceiver implements SignalReceiver
{
	INSTANCE;

	public static final ResourceKey<MapCodec<? extends SignalReceiver>> RESOURCE_KEY = ResourceKey.create(SignalReceiver.REGISTRY_KEY, MoreRed.getModRL("default"));
	public static final MapCodec<DefaultReceiver> CODEC = MapCodec.unit(INSTANCE);
	@Override
	public MapCodec<? extends SignalReceiver> codec()
	{
		return CODEC;
	}
	@Override
	public @Nullable Receiver getReceiverEndpoint(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace, Channel channel)
	{
		return null;
	}
	@Override
	public Collection<Receiver> getAllReceivers(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Channel channel)
	{
		return List.of();
	}
}
