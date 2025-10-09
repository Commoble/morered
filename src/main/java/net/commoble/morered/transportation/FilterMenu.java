package net.commoble.morered.transportation;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class FilterMenu extends AbstractContainerMenu
{
	public final Container inventory;

	// called on Client-side when packet received	
	public static FilterMenu createClientMenu(int id, Inventory playerInventory)
	{
		return new FilterMenu(id, playerInventory, new SimpleContainer(1));
	}
	
	public static MenuConstructor createServerMenuConstructor(FilterBlockEntity filter)
	{
		return (id, playerInventory, theServerPlayer) -> new FilterMenu(id, playerInventory, new FilterInventory(filter));
	}

	private FilterMenu(int id, Inventory playerInventory, Container filterInventory)
	{
		super(MoreRed.FILTER_MENU.get(), id);
		this.inventory = filterInventory;

		// add filter slot
		this.addSlot(new FilterSlot(filterInventory, 0, 80, 35));

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
		return this.inventory.stillValid(playerIn);

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
	
	static class FilterSlot extends Slot
	{
		public FilterSlot(Container inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
		}

		@Override
		public int getMaxStackSize()
		{
			return 1;
		}
	}
	
	static class FilterInventory implements Container
	{
		private final FilterBlockEntity filter;
		
		public FilterInventory(FilterBlockEntity filter)
		{
			this.filter = filter;
		}
		
		@Override
		public void clearContent()
		{
			this.filter.saveAndSync(ItemStack.EMPTY);
		}

		@Override
		public int getContainerSize()
		{
			return 1;
		}

		@Override
		public boolean isEmpty()
		{
			return this.filter.filterStack.isEmpty();
		}

		@Override
		public ItemStack getItem(int index)
		{
			return this.filter.filterStack;
		}

		@Override
		public ItemStack removeItem(int index, int count)
		{
			ItemStack newStack = this.filter.filterStack.split(count);
			this.filter.saveAndSync(this.filter.filterStack);
			return newStack;
		}

		@Override
		public ItemStack removeItemNoUpdate(int index)
		{
			ItemStack stack = this.filter.filterStack.copy();
			this.filter.saveAndSync(ItemStack.EMPTY);
			return stack;
		}

		@Override
		public void setItem(int index, ItemStack stack)
		{
			this.filter.saveAndSync(stack);
		}

		@Override
		public void setChanged()
		{
			this.filter.setChanged();
		}

		@Override
		public boolean stillValid(Player player)
		{
			Level level = this.filter.getLevel();
			BlockPos pos = this.filter.getBlockPos();
			Block block = this.filter.getBlockState().getBlock();
			return level.getBlockState(pos).getBlock() != block ? false : player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
		}
		
	}

}
