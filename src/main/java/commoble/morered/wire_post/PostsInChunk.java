package commoble.morered.wire_post;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import commoble.morered.MoreRed;
import commoble.morered.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

public class PostsInChunk implements ICapabilityProvider, INBTSerializable<CompoundTag>
{
	public static final Capability<PostsInChunk> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final String POSITIONS = "positions";
	public static final Codec<Set<BlockPos>> DATA_CODEC = BlockPos.CODEC.listOf().xmap(HashSet::new, List::copyOf);
	
	private final LazyOptional<PostsInChunk> holder = LazyOptional.of(() -> this);
	
	private Set<BlockPos> positions = new HashSet<>();
	private final LevelChunk chunk; public LevelChunk getChunk() {return this.chunk;}
	
	public PostsInChunk(LevelChunk chunk)
	{
		this.chunk = chunk;
	}
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if (cap == CAPABILITY)
		{
			return CAPABILITY.orEmpty(cap, this.holder);
		}
		else
		{
			return LazyOptional.empty();
		}
	}

	public Set<BlockPos> getPositions()
	{
		return this.positions;
	}

	public void setPositions(Set<BlockPos> set)
	{
		this.positions = set;
		MoreRed.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(this::getChunk), new SyncPostsInChunkPacket(this.chunk.getPos(), set));
	}

	@Override
	public CompoundTag serializeNBT()
	{
		CompoundTag compound = new CompoundTag();
		DATA_CODEC.encodeStart(NbtOps.INSTANCE, this.positions)
			.resultOrPartial(e -> LOGGER.error("Error encoding PostsInChunk for chunk {}, {}: {}", this.chunk.getPos().x, this.chunk.getPos().z, e))
			.ifPresent(tag -> compound.put(POSITIONS, tag));
		return compound;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt)
	{
		Tag tag = nbt.get(POSITIONS);
		if (tag != null)
		{
			DATA_CODEC.parse(NbtOps.INSTANCE, tag)
				.resultOrPartial(e -> LOGGER.error("Error dencoding PostsInChunk for chunk {}, {}: {}", this.chunk.getPos().x, this.chunk.getPos().z, e))
				.ifPresent(set -> this.positions = set);
		}
	}
	
	public static Set<ChunkPos> getRelevantChunkPositionsNearPos(BlockPos pos)
	{
		double range = ServerConfig.INSTANCE.maxWirePostConnectionRange().get();
		ChunkPos chunkPos = new ChunkPos(pos);
		int chunkRange = (int) Math.ceil(range/16D);
		Set<ChunkPos> set = new HashSet<>();
		for (int xOff = -chunkRange; xOff <= chunkRange; xOff++)
		{
			for (int zOff = -chunkRange; zOff <= chunkRange; zOff++)
			{
				set.add(new ChunkPos(chunkPos.x + xOff, chunkPos.z + zOff));
			}
		}
		
		return set;
	}
	
	public void onCapabilityInvalidated()
	{
		this.holder.invalidate();
	}

}
