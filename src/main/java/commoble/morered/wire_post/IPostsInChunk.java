package commoble.morered.wire_post;

import java.util.Set;

import net.minecraft.util.math.BlockPos;

public interface IPostsInChunk
{
	/**
	 * get the mutable set of blockpositions in the chunk (local to the chunk) 
	 * @return The mutable set of blockpositions in the chunk in chunk-local worldspace
	 **/
	public Set<BlockPos> getPositions();
	
	/** 
	 * set a new set of positions to the chunk
	 * @param set The set to set (positions in chunk-local worldspace)
	 **/ 
	public void setPositions(Set<BlockPos> set);
}
