package net.commoble.morered.transportation;

import net.commoble.morered.MoreRed;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class FilterMenu extends AbstractContainerMenu
{
	private final ContainerLevelAccess stillValid;
	private final Block block;
	
	// called on Client-side when packet received	
	public static FilterMenu createClientMenu(int id, Inventory playerInventory)
	{
		return new FilterMenu(id, playerInventory, new ItemStacksResourceHandler(1) {
			@Override
			protected int getCapacity(int index, ItemResource resource)
			{
				return 1;
			}
		}, ContainerLevelAccess.NULL, Blocks.AIR);
	}
	
	public static MenuConstructor createServerMenuConstructor(FilterBlockEntity filter)
	{
		return (id, playerInventory, theServerPlayer) -> new FilterMenu(id, playerInventory, filter.storageHandler, ContainerLevelAccess.create(filter.getLevel(), filter.getBlockPos()), filter.getBlockState().getBlock());
	}

	private FilterMenu(int id, Inventory playerInventory, ItemStacksResourceHandler filterInventory, ContainerLevelAccess stillValid, Block block)
	{
		super(MoreRed.FILTER_MENU.get(), id);
		this.stillValid = stillValid;
		this.block = block;

		// add filter slot
		this.addSlot(new ResourceHandlerSlot(filterInventory, filterInventory::set, 0, 80, 35));

		for (int backpackRow = 0; backpackRow < 3; ++backpackRow)
		{
			for (int backpackColumn = 0; backpackColumn < 9; ++backpackColumn)
			{
				this.addSlot(new Slot(playerInventory, backpackColumn + backpackRow * 9 + 9, 8 + backpackColumn * 18, 84 + backpackRow * 18));
			}
		}

		for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot)
		{
			this.addSlot(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
		}

	}

	@Override
	public boolean stillValid(Player playerIn)
	{
		return AbstractContainerMenu.stillValid(this.stillValid, playerIn, this.block);
	}

	/**
	 * Handle when the stack in slot {@code index} is shift-clicked. Normally this
	 * moves the stack between the player inventory and the other inventory(s).
	 */
	@Override
	public ItemStack quickMoveStack(Player playerIn, int index)
	{
		ItemStack copiedStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem())
		{
			ItemStack stackFromSlot = slot.getItem();
			copiedStack = stackFromSlot.copy();
			if (index == 0)
			{
				if (!this.moveItemStackTo(stackFromSlot, 1, 37, true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if (!this.moveItemStackTo(stackFromSlot, 0, 1, false))
			{
				return ItemStack.EMPTY;
			}

			if (stackFromSlot.isEmpty())
			{
				slot.set(ItemStack.EMPTY);
			}
			else
			{
				slot.setChanged();
			}

			if (stackFromSlot.getCount() == copiedStack.getCount())
			{
				return ItemStack.EMPTY;
			}

			slot.onTake(playerIn, stackFromSlot);
		}

		return copiedStack;
	}

}
