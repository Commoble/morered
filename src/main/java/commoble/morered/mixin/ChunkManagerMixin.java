package commoble.morered.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import commoble.morered.MixinCallbacks;

@Mixin(ChunkMap.class)
public class ChunkManagerMixin {
    @Inject(at = @At(value = "RETURN"), method = "playerLoadedChunk")
    public void afterPlayerLoadedChunk(ServerPlayer player, MutableObject<Packet<?>> packetCache, LevelChunk chunkIn,
                                       CallbackInfo ci) {
        // sync redwire post positions to clients when a chunk needs to be loaded on the client
        MixinCallbacks.afterPlayerLoadedChunk(player, chunkIn);
    }
}
