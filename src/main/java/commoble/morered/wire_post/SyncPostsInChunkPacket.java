package commoble.morered.wire_post;

import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.morered.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

public class SyncPostsInChunkPacket
{
	public static final Codec<ChunkPos> CHUNK_POS_CODEC = Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong);
	public static final Codec<SyncPostsInChunkPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				CHUNK_POS_CODEC.fieldOf("chunk").forGetter(SyncPostsInChunkPacket::getChunkPos),
				PostsInChunk.DATA_CODEC.fieldOf("positions").forGetter(SyncPostsInChunkPacket::getPostsInChunk)
			).apply(instance, SyncPostsInChunkPacket::new));
	
	public static final SyncPostsInChunkPacket BAD_PACKET = new SyncPostsInChunkPacket(new ChunkPos(ChunkPos.INVALID_CHUNK_POS), ImmutableSet.of());
	
	private final ChunkPos chunkPos;	public ChunkPos getChunkPos() { return this.chunkPos; }
	private final Set<BlockPos> inChunk;	public Set<BlockPos> getPostsInChunk() { return this.inChunk; }
	
	public SyncPostsInChunkPacket(ChunkPos chunkPos, Set<BlockPos> inChunk)
	{
		this.chunkPos = chunkPos;
		this.inChunk = inChunk;
	}
	
	public void write(FriendlyByteBuf buffer)
	{
		buffer.writeNbt((CompoundTag)CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElse(new CompoundTag()));
	}
	
	public static SyncPostsInChunkPacket read(FriendlyByteBuf buffer)
	{
		return CODEC.decode(NbtOps.INSTANCE, buffer.readNbt()).result().map(Pair::getFirst).orElse(BAD_PACKET);
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		context.enqueueWork(() -> ClientProxy.updatePostsInChunk(this.chunkPos, this.inChunk));
		context.setPacketHandled(true);
	} 
}
