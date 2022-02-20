package commoble.morered.wire_post;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IPostsInChunk extends INBTSerializable<CompoundTag> {
    /**
     * get the mutable set of blockpositions in the chunk (local to the chunk)
     *
     * @return The mutable set of blockpositions in the chunk in chunk-local worldspace
     **/
    Set<BlockPos> getPositions();

    /**
     * set a new set of positions to the chunk
     *
     * @param set The set to set (positions in chunk-local worldspace)
     **/
    void setPositions(Set<BlockPos> set);
}
