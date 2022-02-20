package commoble.morered;

import commoble.morered.wire_post.PostsInChunkCapability;
import commoble.morered.wire_post.SyncPostsInChunkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;

public class MixinCallbacks {
    // sync redwire post positions to clients when a chunk needs to be loaded on the client
    public static void afterPlayerLoadedChunk(ServerPlayer player, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        chunk.getCapability(PostsInChunkCapability.INSTANCE).ifPresent(cap ->
                MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPostsInChunkPacket(pos, cap.getPositions()))
        );
    }
}
