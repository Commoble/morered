package net.commoble.morered.transportation;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class FilterBlockEntity extends AbstractFilterBlockEntity
{
	public ItemStack filterStack = ItemStack.EMPTY;
	public FilterShuntingItemHandler shuntingHandler = new FilterShuntingItemHandler(this);
	public FilterStorageItemHandler storageHandler = new FilterStorageItemHandler(this);
	
	public FilterBlockEntity(BlockEntityType<?> teType, BlockPos pos, BlockState state)
	{
		super(teType, pos, state);
	}
	
	public FilterBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().filterEntity.get(), pos, state);
	}
	
	@Override
	public boolean canItemPassThroughFilter(ItemStack stack)
	{
		if (stack.getCount() <= 0)
		{
			return false;
		}
		if (this.filterStack.getCount() <= 0)
		{
			return true;
		}
		
		return this.filterStack.getItem().equals(stack.getItem());
	}

	public IItemHandler getItemHandler(@Nullable Direction side)
	{
		Direction output_dir = this.getBlockState().getValue(ShuntBlock.FACING);
		if (side == output_dir.getOpposite())
		{
			return this.shuntingHandler;
		}
		else if (side != output_dir)
		{
			return this.storageHandler;
		}
		return null;
	}
	
	public void setFilterStackAndSaveAndSync(ItemStack filterStack)
	{
		this.filterStack = filterStack;
		this.setChanged();
		BlockState state = this.getBlockState();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), state, state, 2);
		
	}
	
	public void dropItems()
	{
		BlockPos thisPos = this.getBlockPos();
		Containers.dropItemStack(this.getLevel(), thisPos.getX(), thisPos.getY(), thisPos.getZ(), this.filterStack);
	}
	
	////// NBT and syncing

	@Override	// write entire inventory by default (for server -> hard disk purposes this is what is called)
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		if (!this.filterStack.isEmpty())
		{
			Tag inventory = this.filterStack.save(registries);
			compound.put(INV_KEY, inventory);
		}
	}
	
	@Override
	/** read **/
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries);
		CompoundTag inventory = compound.getCompoundOrEmpty(INV_KEY);
		this.filterStack = ItemStack.parse(registries, inventory).orElse(ItemStack.EMPTY);
	}
	
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag tag = super.getUpdateTag(registries);
		Tag inventory = this.filterStack.isEmpty()
			? new CompoundTag()
			: filterStack.save(registries);
		tag.put(INV_KEY, inventory);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}	
	
	// super.handleUpdateTag() just calls load()
	
	// super.onDataPacket() just calls load()
}
