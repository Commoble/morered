package commoble.morered;

import commoble.morered.wire_post.PostsInChunkCapability;
import commoble.morered.wire_post.SyncPostsInChunkPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.PacketDistributor;

public class MixinCallbacks
{
	// sync redwire post positions to clients when a chunk needs to be loaded on the client
	public static void afterSendChunkData(ServerPlayerEntity player, Chunk chunk)
	{
		ChunkPos pos = chunk.getPos();
		chunk.getCapability(PostsInChunkCapability.INSTANCE).ifPresent(cap -> 
			MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(()->player), new SyncPostsInChunkPacket(pos, cap.getPositions()))
		);
	}
}
