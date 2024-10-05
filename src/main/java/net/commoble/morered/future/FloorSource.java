package net.commoble.morered.future;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public record FloorSource(int offset) implements SignalSource
{
	public static final ResourceKey<MapCodec<? extends SignalSource>> RESOURCE_KEY = ResourceKey.create(SignalSource.REGISTRY_KEY, MoreRed.getModRL("floor"));
	public static final MapCodec<FloorSource> CODEC = Codec.INT.optionalFieldOf("offset", 0).xmap(FloorSource::new, FloorSource::offset);

	@Override
	public MapCodec<? extends SignalSource> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace)
	{
		if (connectedFace.attachmentSide() != Direction.DOWN)
			return Map.of();

		BlockPos offsetFromNeighbor = supplierPos.subtract(connectedFace.pos());
		@Nullable Direction directionFromNeighbor = Direction.fromDelta(offsetFromNeighbor.getX(), offsetFromNeighbor.getY(), offsetFromNeighbor.getZ()); 
		return directionFromNeighbor == null
			? Map.of()
			: Map.of(Channel.wide(), reader -> reader.getSignal(supplierPos, directionFromNeighbor) + this.offset);
	}
}
