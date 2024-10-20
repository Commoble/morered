package net.commoble.morered.wires;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.Receiver;
import net.commoble.exmachina.api.SignalReceiver;
import net.commoble.morered.MoreRed;
import net.commoble.morered.ObjectNames;
import net.commoble.morered.bitwise_logic.BitewiseGateBlock;
import net.commoble.morered.bitwise_logic.BitwiseGateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public enum BitwiseGateReceiver implements SignalReceiver
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalReceiver>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_RECEIVER_TYPE, MoreRed.getModRL(ObjectNames.BITWISE_GATE));
	public static final MapCodec<BitwiseGateReceiver> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalReceiver> codec()
	{
		return CODEC;
	}

	@Override
	public @Nullable Receiver getReceiverEndpoint(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide,
		Face connectedFace, Channel channel)
	{
		if (receiverState.getBlock() instanceof BitewiseGateBlock block)
		{
			return block.getReceiverEndpoint(level, receiverPos, receiverState, receiverSide, connectedFace, channel);
		}
		return null;
	}

	@Override
	public Collection<Receiver> getAllReceivers(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Channel channel)
	{
		if (receiverState.getBlock() instanceof BitewiseGateBlock block)
		{
			return block.getAllReceivers(level, receiverPos, receiverState, channel);
		}
		return List.of();
	}

	@Override
	public void resetUnusedReceivers(LevelAccessor level, BlockPos pos, Collection<Receiver> receivers)
	{
		List<BitwiseGateBlockEntity.BitwiseListener> listeners = new ArrayList<>();
		for (Receiver receiver : receivers)
		{
			if (receiver instanceof BitwiseGateBlockEntity.BitwiseListener listener)
			{
				listeners.add(listener);
			}
		}
		if (level.getBlockEntity(pos) instanceof BitwiseGateBlockEntity gate)
		{
			gate.resetUnusedReceivers(listeners);
		}
	}
}
