package net.commoble.morered.transportation;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class MultiFilterBlockEntity extends AbstractFilterBlockEntity
{
	public final SetItemHandler inventory = new SetItemHandler(27) {
		@Override
		protected void onContentsChanged(int slot)
		{
			super.onContentsChanged(slot);
			MultiFilterBlockEntity.this.setChanged();
		}
	};
	public final IItemHandler shuntingHandler = new FilterShuntingItemHandler(this); 
	
	public MultiFilterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public MultiFilterBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().multiFilterEntity.get(), pos, state);
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side)
	{
		if (this.getBlockState().hasProperty(DirectionalBlock.FACING))
		{
			Direction outputDir = this.getBlockState().getValue(DirectionalBlock.FACING);
			if (side != outputDir)
			{
				return this.shuntingHandler;
			}
		}
		return null;
	}

	@Override
	public boolean canItemPassThroughFilter(ItemStack stack)
	{
		return this.inventory.getSet().contains(stack.getItem());
	}

	@Override
	public void dropItems()
	{
		int slots = this.inventory.getSlots();
		BlockPos pos = this.getBlockPos();
		for (int i=0; i<slots; i++)
		{
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.inventory.getStackInSlot(i));
		}
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		this.inventory.deserializeNBT(registries, tag.getCompound(INV_KEY));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.put(INV_KEY, this.inventory.serializeNBT(registries));
	}
}
