package net.commoble.morered.future;

import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public record WireIgnoringSignalGetter(LevelReader delegate, Function<BlockPos, StateWirer> wirerLookup) implements SignalGetter
{

	@Override
	public BlockEntity getBlockEntity(BlockPos pos)
	{
		return delegate.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos)
	{
		return delegate.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos)
	{
		return delegate.getFluidState(pos);
	}

	@Override
	public int getHeight()
	{
		return delegate.getHeight();
	}

	@Override
	public int getMinBuildHeight()
	{
		return delegate.getMinBuildHeight();
	}

	@Override
	public int getDirectSignal(BlockPos pos, Direction directionFromNeighbor)
	{
		StateWirer stateWirer = wirerLookup.apply(pos);
		return stateWirer.ignoreVanillaSignal(this.delegate)
			? 0
			: SignalGetter.super.getDirectSignal(pos, directionFromNeighbor);
	}

	@Override
	public int getSignal(BlockPos pos, Direction directionFromNeighbor)
	{
		StateWirer stateWirer = wirerLookup.apply(pos);
		return stateWirer.ignoreVanillaSignal(this.delegate)
			? 0
			: SignalGetter.super.getSignal(pos, directionFromNeighbor);
	}
}
