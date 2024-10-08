package net.commoble.morered.api;
import java.util.Collection;

import org.jetbrains.annotations.Nullable;

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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public interface SignalReceiver
{
	public static final ResourceKey<Registry<MapCodec<? extends SignalReceiver>>> REGISTRY_KEY = ResourceKey.createRegistryKey(MoreRed.getModRL("signal_receiver"));
	public static final Codec<SignalReceiver> CODEC = MoreCodecs.dispatch(REGISTRY_KEY, SignalReceiver::codec);
	DataMapType<Block, SignalReceiver> DATA_MAP_TYPE = DataMapType.builder(MoreRed.getModRL("signal_receiver"), Registries.BLOCK, CODEC).build();

	public abstract MapCodec<? extends SignalReceiver> codec();
	public abstract @Nullable Receiver getReceiverEndpoint(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace, Channel channel);
	public abstract Collection<Receiver> getAllReceivers(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Channel channel);
	
	public default void resetUnusedReceivers(LevelAccessor level, BlockPos pos, Collection<Receiver> receivers)
	{
		for (Receiver receiver : receivers)
		{
			receiver.accept(level, 0);
		}
	}
}
