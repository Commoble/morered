package commoble.morered.wire_post;

import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PostsInChunkAttacher {

    private static class PostsInChunkProvider implements ICapabilitySerializable<CompoundTag> {

        public static final ResourceLocation IDENTIFIER = MoreRed.getModRL(ObjectNames.POSTS_IN_CHUNK);

        private final IPostsInChunk backend;
        private final LazyOptional<IPostsInChunk> optionalData;

        public PostsInChunkProvider(LevelChunk chunk) {
            backend = new PostsInChunk(chunk);
            optionalData = LazyOptional.of(() -> backend);
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return PostsInChunkCapability.INSTANCE.orEmpty(cap, optionalData);
        }

        void invalidate() {
            optionalData.invalidate();
        }

        @Override
        public CompoundTag serializeNBT() {
            return backend.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            backend.deserializeNBT(nbt);
        }
    }

    public static void attach(final AttachCapabilitiesEvent<LevelChunk> event) {
        final PostsInChunkProvider provider = new PostsInChunkProvider(event.getObject());
        event.addCapability(PostsInChunkProvider.IDENTIFIER, provider);
        event.addListener(provider::invalidate);
    }

    private PostsInChunkAttacher() {
    }
}
