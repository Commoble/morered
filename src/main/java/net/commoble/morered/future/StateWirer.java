package net.commoble.morered.future;

import java.util.Objects;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public record StateWirer(BlockState state, SignalSource source, SignalTransmitter transmitter, SignalReceiver receiver)
{
	@SuppressWarnings("deprecation")
	public static StateWirer getOrDefault(BlockGetter blockGetter, BlockPos pos)
	{
		BlockState state = blockGetter.getBlockState(pos);
		SignalSource source = Objects.requireNonNullElse(
			BuiltInRegistries.BLOCK.getData(ExperimentalModEvents.SIGNAL_SOURCE_DATA_MAP, state.getBlock().builtInRegistryHolder().getKey()),
			DefaultSource.INSTANCE);
		SignalTransmitter transmitter = Objects.requireNonNullElse(
			BuiltInRegistries.BLOCK.getData(ExperimentalModEvents.SIGNAL_TRANSMITTER_DATA_MAP, state.getBlock().builtInRegistryHolder().getKey()),
			DefaultTransmitter.INSTANCE);
		SignalReceiver receiver = Objects.requireNonNullElse(
			BuiltInRegistries.BLOCK.getData(ExperimentalModEvents.SIGNAL_RECEIVER_DATA_MAP, state.getBlock().builtInRegistryHolder().getKey()),
			DefaultReceiver.INSTANCE);
		return new StateWirer(state, source, transmitter, receiver);
	}
	
	public boolean ignoreVanillaSignal(LevelReader levelReader)
	{
		return this.state.is(MoreRed.Tags.Blocks.IGNORE_VANILLA_SIGNAL);
	}
}
