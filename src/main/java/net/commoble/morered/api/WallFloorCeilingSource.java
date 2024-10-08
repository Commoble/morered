package net.commoble.morered.api;

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
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public record WallFloorCeilingSource(boolean invertHorizontalFacing) implements SignalSource
{
	public static final ResourceKey<MapCodec<? extends SignalSource>> RESOURCE_KEY = ResourceKey.create(SignalSource.REGISTRY_KEY, MoreRed.getModRL("wall_floor_ceiling"));
	public static final MapCodec<WallFloorCeilingSource> CODEC = Codec.BOOL.optionalFieldOf("invert_horizontal_facing", false).xmap(WallFloorCeilingSource::new, WallFloorCeilingSource::invertHorizontalFacing);

	@Override
	public MapCodec<? extends SignalSource> codec()
	{
		return CODEC;
	}

	@Override
	public Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide, Face connectedFace)
	{
		Direction wireSide = connectedFace.attachmentSide();
		if (supplierState.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE))
		{
			AttachFace attachFace = supplierState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
			if (attachFace == AttachFace.FLOOR && wireSide == Direction.DOWN
				|| attachFace == AttachFace.CEILING && wireSide == Direction.UP)
			{
				BlockPos offsetFromNeighbor = supplierPos.subtract(connectedFace.pos());
				@Nullable Direction directionFromNeighbor = Direction.fromDelta(offsetFromNeighbor.getX(), offsetFromNeighbor.getY(), offsetFromNeighbor.getZ()); 
				return directionFromNeighbor == null
					? Map.of()
					: Map.of(Channel.wide(), reader -> reader.getSignal(supplierPos, directionFromNeighbor));
			}
			if (attachFace != AttachFace.WALL)
				return Map.of();
		}
		if (!supplierState.hasProperty(HorizontalDirectionalBlock.FACING))
			return Map.of();

		Direction facing = supplierState.getValue(HorizontalDirectionalBlock.FACING);
		if (invertHorizontalFacing)
			facing = facing.getOpposite();
		
		if (connectedFace.attachmentSide() != facing)
			return Map.of();
		
		BlockPos offsetFromNeighbor = supplierPos.subtract(connectedFace.pos());
		@Nullable Direction directionFromNeighbor = Direction.fromDelta(offsetFromNeighbor.getX(), offsetFromNeighbor.getY(), offsetFromNeighbor.getZ()); 
		return directionFromNeighbor == null
			? Map.of()
			: Map.of(Channel.wide(), reader -> reader.getSignal(supplierPos, directionFromNeighbor));
	}
}
