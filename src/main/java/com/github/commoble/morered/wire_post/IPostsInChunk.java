package com.github.commoble.morered.wire_post;

import java.util.Set;

import net.minecraft.util.math.BlockPos;

public interface IPostsInChunk
{
	/** get the mutable set of blockpositions in the chunk (local to the chunk) **/
	public Set<BlockPos> getPositions();
	
	/** set a new set of positions to the chunk **/ 
	public void setPositions(Set<BlockPos> set);
}
