package net.commoble.morered;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import net.commoble.morered.util.SnapshotStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public record ExtractOnlyGenericItemHandler(GenericBlockEntity be, DataComponentType<ItemContainerContents> type, int size, SnapshotStack<ItemContainerContents> inventoryStack) implements ResourceHandler<ItemResource>, IndexModifier<ItemResource>
{	
	public static ExtractOnlyGenericItemHandler of(GenericBlockEntity be, DataComponentType<ItemContainerContents> type, int size)
	{
		return new ExtractOnlyGenericItemHandler(
			be,
			type,
			size,
			SnapshotStack.of(
				be.getOrDefault(type, ItemContainerContents.EMPTY),
				UnaryOperator.identity(), // ItemContainerContents is immutable, don't need to deep-copy
				(oldContents,newContents) -> be.set(type, newContents)));
	}

	@Override
	public ItemResource getResource(int index)
	{
		ItemContainerContents inventory = this.inventoryStack.get();
		return index < inventory.getSlots()
			? ItemResource.of(inventory.getStackInSlot(index))
			: ItemResource.EMPTY;
	}

	@Override
	public long getAmountAsLong(int index)
	{
		ItemContainerContents inventory = this.inventoryStack.get();
		return index < inventory.getSlots()
			? inventory.getStackInSlot(index).getCount()
			: 0;
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource)
	{
		ItemResource existing = this.getResource(index);
		return existing.isEmpty()
			? resource.getMaxStackSize()
			: existing.getMaxStackSize();
	}

	@Override
	public boolean isValid(int index, ItemResource resource)
	{
		return false;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		return 0; // can't insert
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		ItemContainerContents oldInventory = this.inventoryStack.get();
		if (index >= oldInventory.getSlots())
			return 0;
		
		ItemStack stackInSlot = oldInventory.getStackInSlot(index);
		int oldCount = stackInSlot.getCount();
		int extracted = Math.min(amount, oldCount);
		int newCount = oldCount - extracted;
		ItemStack newStack = newCount > 0
			? stackInSlot.copyWithCount(newCount)
			: ItemStack.EMPTY;
		
		// ItemContainerContents#stream copies itemstacks
		List<ItemStack> newList = oldInventory.stream().collect(Collectors.toCollection(ArrayList::new));
		newList.set(index, newStack);
		ItemContainerContents newInventory = ItemContainerContents.fromItems(newList);
		this.inventoryStack.setAndTakeSnapshot(newInventory, transaction);
		return extracted;
	}

	@Override
	public void set(int index, ItemResource resource, int amount)
	{
		ItemContainerContents inventory = this.inventoryStack.get();
		if (index >= inventory.getSlots())
			return;
        NonNullList<ItemStack> list = NonNullList.withSize(inventory.getSlots(), ItemStack.EMPTY);
        inventory.copyInto(list);
        list.set(index, resource.toStack(amount));
		be.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
	}
}
