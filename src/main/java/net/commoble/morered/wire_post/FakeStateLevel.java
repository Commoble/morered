package net.commoble.morered.wire_post;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * BlockGetter that delegates to another reader for most purposes,
 * except that a state at a given position will appear to be a different given state
 */
public class FakeStateLevel implements BlockGetter
{
	private final BlockGetter delegate;
	private final BlockPos pos;
	private final BlockState state;
	
	public FakeStateLevel(BlockGetter delegate, BlockPos pos, BlockState state)
	{
		this.delegate = delegate;
		this.pos = pos.immutable();
		this.state = state;
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos)
	{
		return this.delegate.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos)
	{
		return pos.equals(this.pos) ? this.state : this.delegate.getBlockState(pos); 
	}

	@Override
	public FluidState getFluidState(BlockPos pos)
	{
		return this.delegate.getFluidState(pos);
	}

	@Override
	public int getHeight()
	{
		return this.delegate.getHeight();
	}

	@Override
	public int getMinBuildHeight()
	{
		return this.delegate.getMinBuildHeight();
	}
	
}
