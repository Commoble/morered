package net.commoble.morered.transportation;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		output.store(INV_KEY, ItemStack.OPTIONAL_CODEC, this.filterStack);
	}
	
	@Override
	/** read **/
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		this.filterStack = input.read(INV_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
	}
	
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		return this.saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}	
	
	// super.handleUpdateTag() just calls load()
	
	// super.onDataPacket() just calls load()
}
