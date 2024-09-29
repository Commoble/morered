package net.commoble.morered.future;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public record FloorWirer(int offset) implements Wirer
{
	public static final ResourceKey<MapCodec<? extends Wirer>> RESOURCE_KEY = ResourceKey.create(Wirer.REGISTRY_KEY, MoreRed.getModRL("floor"));
	public static final MapCodec<FloorWirer> CODEC = Codec.INT.optionalFieldOf("offset", 0).xmap(FloorWirer::new, FloorWirer::offset);

	@Override
	public MapCodec<? extends Wirer> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		return Map.of();
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

	@Override
	public Map<Channel,BiConsumer<LevelAccessor,Integer>> getReceiverEndpoints(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace)
	{
		return Map.of();
	}
	
}
