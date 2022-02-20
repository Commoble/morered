package commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import commoble.morered.MoreRed;
import commoble.morered.foundation.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;

public class PostsInChunk implements IPostsInChunk {
//	private final LazyOptional<IPostsInChunk> holder = LazyOptional.of(() -> this);

    public static final String POSITIONS = "positions";

    private Set<BlockPos> positions = new HashSet<>();
    private final LevelChunk chunk;

    public LevelChunk getChunk() {
        return this.chunk;
    }

    public PostsInChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

//	@Override
//	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
//		if (cap == PostsInChunkCapability.INSTANCE) {
//			return PostsInChunkCapability.INSTANCE.orEmpty(cap, this.holder);
//		} else {
//			return LazyOptional.empty();
//		}
//	}

    @Override
    public Set<BlockPos> getPositions() {
        return this.positions;
    }

    @Override
    public void setPositions(Set<BlockPos> set) {
        this.positions = set;
        MoreRed.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(this::getChunk),
                new SyncPostsInChunkPacket(this.chunk.getPos(), set));
    }

    @Override
    public CompoundTag serializeNBT() {
//		return (CompoundTag)PostsInChunkCapability.INSTANCE.getStorage().writeNBT(PostsInChunkCapability.INSTANCE,
//		this, null);
        CompoundTag compoundTag = new CompoundTag();
//		this.positions.forEach((blockPos -> compoundTag.put(PostsInChunkCapability.INSTANCE, NbtUtils.writeBlockPos
//		(blockPos))));
        ListTag list = new ListTag();
        positions.forEach(blockPos -> list.add(NbtUtils.writeBlockPos(blockPos)));
        compoundTag.put(POSITIONS, list);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
//		PostsInChunkCapability.INSTANCE.getStorage().readNBT(PostsInChunkCapability.INSTANCE, this, null, nbt);
        ListTag list = nbt.getList(POSITIONS, Tag.TAG_COMPOUND);
        List<BlockPos> p = new ArrayList<>();
        list.forEach(tag -> p.add(NbtUtils.readBlockPos((CompoundTag) tag)));
        positions = new HashSet<>(p);
    }

    public static Set<ChunkPos> getRelevantChunkPositionsNearPos(BlockPos pos) {
        double range = AllConfigs.SERVER.maxWirePostConnectionRange.getD();
        ChunkPos chunkPos = new ChunkPos(pos);
        int chunkRange = (int) Math.ceil(range / 16D);
        Set<ChunkPos> set = new HashSet<>();
        for (int xOff = -chunkRange; xOff <= chunkRange; xOff++) {
            for (int zOff = -chunkRange; zOff <= chunkRange; zOff++) {
                set.add(new ChunkPos(chunkPos.x + xOff, chunkPos.z + zOff));
            }
        }

        return set;
    }

//	public void onCapabilityInvalidated()
//	{
//		this.holder.invalidate();
//	}

}
