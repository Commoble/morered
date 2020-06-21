package com.github.commoble.morered.wire_post;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PostsInChunk implements IPostsInChunk, ICapabilityProvider, INBTSerializable<CompoundNBT>
{	
	private final LazyOptional<IPostsInChunk> holder = LazyOptional.of(() -> this);
	
	private Set<BlockPos> positions = new HashSet<>();
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if (cap == PostsInChunkCapability.INSTANCE)
		{
			return PostsInChunkCapability.INSTANCE.orEmpty(cap, this.holder);
		}
		else
		{
			return LazyOptional.empty();
		}
	}

	@Override
	public Set<BlockPos> getPositions()
	{
		return this.positions;
	}

	@Override
	public void setPositions(Set<BlockPos> set)
	{
		this.positions = set;
	}

	@Override
	public CompoundNBT serializeNBT()
	{
		return (CompoundNBT)PostsInChunkCapability.INSTANCE.getStorage().writeNBT(PostsInChunkCapability.INSTANCE, this, null);
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt)
	{
		PostsInChunkCapability.INSTANCE.getStorage().readNBT(PostsInChunkCapability.INSTANCE, this, null, nbt);
	}

}
