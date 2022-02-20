package commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent.Context;

public class SyncPostsInChunkPacket {

    private final ChunkPos pos;
    private final Set<BlockPos> posts;

    public SyncPostsInChunkPacket(ChunkPos pos, Set<BlockPos> posts) {
        this.pos = pos;
        this.posts = posts;
    }

    public SyncPostsInChunkPacket(FriendlyByteBuf buffer) {
        this.pos = new ChunkPos(buffer.readInt(), buffer.readInt());
        CompoundTag nbt = buffer.readNbt();
        ListTag list = nbt.getList(PostsInChunk.POSITIONS, Tag.TAG_COMPOUND);
        List<BlockPos> p = new ArrayList<>();
        list.forEach(tag -> p.add(NbtUtils.readBlockPos((CompoundTag) tag)));
        posts = new HashSet<>(p);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(pos.x);
        buffer.writeInt(pos.z);
        CompoundTag compoundTag = new CompoundTag();
        ListTag list = new ListTag();
        posts.forEach(blockPos -> list.add(NbtUtils.writeBlockPos(blockPos)));
        compoundTag.put(PostsInChunk.POSITIONS, list);
        buffer.writeNbt(compoundTag);
    }

    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            MoreRed.CLIENT_PROXY.ifPresent(clientProxy -> context.get().enqueueWork(() -> clientProxy.updatePostsInChunk(pos, posts)));
        });
        context.get().setPacketHandled(true);
    }

//	public static final Codec<SyncPostsInChunkPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
//			ExtraCodecs.COMPRESSED_CHUNK_POS.fieldOf("chunk").forGetter(SyncPostsInChunkPacket::getChunkPos),
//				PostsInChunkCapability.POST_SET_CODEC.fieldOf("tubes").forGetter
//				(SyncPostsInChunkPacket::getPostsInChunk)
//			).apply(instance, SyncPostsInChunkPacket::new));
//
//	public static final SyncPostsInChunkPacket BAD_PACKET = new SyncPostsInChunkPacket(new ChunkPos(ChunkPos
//	.INVALID_CHUNK_POS), ImmutableSet.of());
//
//	private final ChunkPos chunkPos;	public ChunkPos getChunkPos() { return this.chunkPos; }
//	private final Set<BlockPos> inChunk;	public Set<BlockPos> getPostsInChunk() { return this.inChunk; }
//
//	public SyncPostsInChunkPacket(ChunkPos chunkPos, Set<BlockPos> inChunk)
//	{
//		this.chunkPos = chunkPos;
//		this.inChunk = inChunk;
//	}
//
//	public void write(FriendlyByteBuf buffer)
//	{
//		buffer.writeNbt((CompoundTag)CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElse(new CompoundTag()));
//	}
//
//	public static SyncPostsInChunkPacket read(FriendlyByteBuf buffer)
//	{
//		return CODEC.decode(NbtOps.INSTANCE, buffer.readNbt()).result().map(Pair::getFirst).orElse(BAD_PACKET);
//	}
//
//	public void handle(Supplier<NetworkEvent.Context> contextGetter)
//	{
//		NetworkEvent.Context context = contextGetter.get();
//		MoreRed.CLIENT_PROXY.ifPresent(proxy -> contextGetter.get().enqueueWork(() -> proxy.updatePostsInChunk(this.chunkPos, this
//		.inChunk)));
//		context.setPacketHandled(true);
//	}
}
