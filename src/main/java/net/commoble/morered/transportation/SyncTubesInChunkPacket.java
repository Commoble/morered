package net.commoble.morered.transportation;

import java.util.List;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import net.commoble.morered.client.ClientProxy;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncTubesInChunkPacket(ChunkPos chunkPos, Set<BlockPos> tubesInChunk) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SyncTubesInChunkPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.id("sync_tubes_in_chunk"));
	
	public static final StreamCodec<ByteBuf, SyncTubesInChunkPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_LONG.map(ChunkPos::new, ChunkPos::toLong), SyncTubesInChunkPacket::chunkPos,
		BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf), SyncTubesInChunkPacket::tubesInChunk,
		SyncTubesInChunkPacket::new);

	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> ClientProxy.updateTubesInChunk(this.chunkPos, this.tubesInChunk));
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
