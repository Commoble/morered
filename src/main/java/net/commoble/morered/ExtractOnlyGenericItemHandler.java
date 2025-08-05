package net.commoble.morered;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public record ExtractOnlyGenericItemHandler(GenericBlockEntity be, DataComponentType<ItemContainerContents> type, int slots) implements IItemHandler, IItemHandlerModifiable
{
	private ItemContainerContents getInventory()
	{
		return be.getOrDefault(type, ItemContainerContents.EMPTY);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack)
	{
		ItemContainerContents inventory = this.getInventory();
		if (slot >= inventory.getSlots())
			return;
        NonNullList<ItemStack> list = NonNullList.withSize(inventory.getSlots(), ItemStack.EMPTY);
        inventory.copyInto(list);
        list.set(slot, stack);
		be.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
	}
	
	@Override
	public int getSlots()
	{
		return slots;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		var inventory = getInventory();
		return slot < inventory.getSlots()
			? inventory.getStackInSlot(slot)
			: ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		return stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		ItemContainerContents oldInventory = getInventory();
		if (slot >= oldInventory.getSlots())
			return ItemStack.EMPTY;
		ItemStack stackInSlot = oldInventory.getStackInSlot(slot);
		int oldCount = stackInSlot.getCount();
		if (oldCount >= amount)
		{
			// return the entire stack
			if (!simulate)
			{
				setStackInSlot(slot, ItemStack.EMPTY);
			}
			return stackInSlot;
		}
		else
		{
			// return the requested portion of the stack
			ItemStack out = stackInSlot.copyWithCount(amount);
			if (!simulate)
			{
				ItemStack remainder = stackInSlot.copyWithCount(oldCount - amount);
				setStackInSlot(slot, remainder);
			}
			return out;
		}
	}

	@Override
	public int getSlotLimit(int slot)
	{
		ItemContainerContents inventory = getInventory();
		return slot < inventory.getSlots()
			? inventory.getStackInSlot(slot).getMaxStackSize()
			: 0;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack)
	{
		return false;
	}
}
