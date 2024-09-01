package net.commoble.morered.wire_post;

import java.util.List;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import net.commoble.morered.MoreRed;
import net.commoble.morered.ObjectNames;
import net.commoble.morered.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPostsInChunkPacket(ChunkPos chunkPos, Set<BlockPos> postsInChunk) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SyncPostsInChunkPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.getModRL(ObjectNames.POSTS_IN_CHUNK));
	public static final StreamCodec<ByteBuf, SyncPostsInChunkPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_LONG.map(ChunkPos::new, ChunkPos::toLong), SyncPostsInChunkPacket::chunkPos,
		BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Set::copyOf,List::copyOf), SyncPostsInChunkPacket::postsInChunk,
		SyncPostsInChunkPacket::new);
	
	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> ClientProxy.updatePostsInChunk(this.chunkPos, this.postsInChunk));
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	} 
}
