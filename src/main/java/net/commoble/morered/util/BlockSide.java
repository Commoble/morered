package net.commoble.morered.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;

public record BlockSide (BlockPos pos, Direction direction)
{
	public static final Codec<BlockSide> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(BlockSide::pos),
			Direction.CODEC.fieldOf("direction").forGetter(BlockSide::direction)
		).apply(builder, BlockSide::new));
	
	public static final StreamCodec<ByteBuf, BlockSide> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, BlockSide::pos,
		Direction.STREAM_CODEC, BlockSide::direction,
		BlockSide::new);
}