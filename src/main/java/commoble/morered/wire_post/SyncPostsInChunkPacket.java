package commoble.morered.wire_post;

import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.databuddy.codec.ExtraCodecs;
import commoble.morered.MoreRed;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class SyncPostsInChunkPacket
{
	public static final Codec<SyncPostsInChunkPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.COMPRESSED_CHUNK_POS.fieldOf("chunk").forGetter(SyncPostsInChunkPacket::getChunkPos),
				PostsInChunkCapability.POST_SET_CODEC.fieldOf("tubes").forGetter(SyncPostsInChunkPacket::getPostsInChunk)
			).apply(instance, SyncPostsInChunkPacket::new));
	
	public static final SyncPostsInChunkPacket BAD_PACKET = new SyncPostsInChunkPacket(new ChunkPos(ChunkPos.SENTINEL), ImmutableSet.of());
	
	private final ChunkPos chunkPos;	public ChunkPos getChunkPos() { return this.chunkPos; }
	private final Set<BlockPos> inChunk;	public Set<BlockPos> getPostsInChunk() { return this.inChunk; }
	
	public SyncPostsInChunkPacket(ChunkPos chunkPos, Set<BlockPos> inChunk)
	{
		this.chunkPos = chunkPos;
		this.inChunk = inChunk;
	}
	
	public void write(PacketBuffer buffer)
	{
		buffer.writeCompoundTag((CompoundNBT)CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElse(new CompoundNBT()));
	}
	
	public static SyncPostsInChunkPacket read(PacketBuffer buffer)
	{
		return CODEC.decode(NBTDynamicOps.INSTANCE, buffer.readCompoundTag()).result().map(Pair::getFirst).orElse(BAD_PACKET);
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		MoreRed.CLIENT_PROXY.ifPresent(proxy -> contextGetter.get().enqueueWork(() -> proxy.updatePostsInChunk(this.chunkPos, this.inChunk)));
		context.setPacketHandled(true);
	} 
}
