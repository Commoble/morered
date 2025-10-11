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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

public class FilterBlockEntity extends AbstractFilterBlockEntity
{
	public FilterShuntingItemHandler shuntingHandler = new FilterShuntingItemHandler(this);
	public FilterStorageItemHandler storageHandler = new FilterStorageItemHandler(this);
	
	public FilterBlockEntity(BlockEntityType<?> teType, BlockPos pos, BlockState state)
	{
		super(teType, pos, state);
	}
	
	public FilterBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.FILTER_BLOCK_ENTITY.get(), pos, state);
	}
	
	public ItemStack filterStack()
	{
		return ItemUtil.getStack(this.storageHandler, 0);
	}
	
	@Override
	public boolean canItemPassThroughFilter(Item item)
	{
		ItemStack filterStack = this.filterStack();
		if (filterStack.getCount() <= 0)
		{
			return true;
		}
		
		return filterStack.getItem() == item;
	}

	public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side)
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
	
	public void saveAndSync()
	{
		this.setChanged();
		BlockState state = this.getBlockState();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), state, state, 2);
		
	}
	
	public void dropItems()
	{
		ItemStack filterStack = this.filterStack();
		if (!filterStack.isEmpty())
		{
			BlockPos thisPos = this.getBlockPos();
			Containers.dropItemStack(this.getLevel(), thisPos.getX(), thisPos.getY(), thisPos.getZ(), filterStack);	
		}
	}
	
	////// NBT and syncing

	@Override	// write entire inventory by default (for server -> hard disk purposes this is what is called)
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		this.storageHandler.serialize(output);
	}
	
	@Override
	/** read **/
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		this.storageHandler.deserialize(input);
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
