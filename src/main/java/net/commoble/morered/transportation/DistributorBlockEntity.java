package net.commoble.morered.transportation;

import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class DistributorBlockEntity extends BlockEntity
{
	public static final String NEXT_DIRECTIONS = "next_directions";
	
	// handler instances for internal use
	// index of array represents the face of this tile that items are inserted into
	protected final DistributorItemHandler[] handlers = IntStream.range(0, 6)
		.mapToObj(i -> new DistributorItemHandler(this, Direction.from3DDataValue(i)))
		.toArray(DistributorItemHandler[]::new);
	
	public Direction nextSide = Direction.DOWN;
	
	public DistributorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public DistributorBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().distributorEntity.get(), pos, state);
	}
	
	public IItemHandler getItemHandler(@Nullable Direction side)
	{
		return side == null ? null : this.handlers[side.ordinal()];
	}
	
	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries)
	{
		super.saveAdditional(nbt, registries);
		nbt.putIntArray(NEXT_DIRECTIONS, IntStream.range(0, 6)
			.map(i-> this.handlers[i].getNextDirectionIndex())
			.toArray());
	}
	
	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries)
	{
		int[] directionIndices = nbt.getIntArray(NEXT_DIRECTIONS);
		int maxSize = Math.min(this.handlers.length, directionIndices.length);
		for (int i=0; i<maxSize; i++)
		{
			this.handlers[i].setNextDirectionIndex(directionIndices[i]);
		}
		super.loadAdditional(nbt, registries);
	}
}
