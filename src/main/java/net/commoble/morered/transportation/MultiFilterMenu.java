package net.commoble.morered.transportation;

import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Predicates;

import net.commoble.morered.MoreRed;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MultiFilterMenu extends AbstractContainerMenu
{
	public static final int ROWS = 3;
	public static final int COLS = 9;
	public static final int INVENTORY_SIZE = 27;
	
	private final Predicate<Player> validator;
	
	public static MultiFilterMenu clientMenu(int id, Inventory playerInventory)
	{
		return new MultiFilterMenu(id, playerInventory, Predicates.alwaysTrue(), new SetItemHandler(INVENTORY_SIZE));
	}
	
	public static MenuConstructor serverMenu(MultiFilterBlockEntity filter)
	{
		return (id, playerInventory, player) -> new MultiFilterMenu(id, playerInventory, p -> Container.stillValidBlockEntity(filter, p), filter.inventory);
	}

	protected MultiFilterMenu(int id, Inventory playerInventory, Predicate<Player> validator, SetItemHandler filterInventory)
	{
		super(MoreRed.MULTI_FILTER_MENU.get(), id);
		this.validator = validator;
		
		int rows = 3;
		int cols = 9;
		
		// block inventory
		for (int row=0; row<rows; row++)
		{
			for (int col=0; col<cols; col++)
			{
				int index = row*cols + col;
				int x = col*18 + 8;
				int y = row*18 + 18;
				this.addSlot(new MultiFilterSlot(filterInventory, index, x, y));
			}
		}
		
		// player inventory
		for (int row=0; row<rows; row++)
		{
			for (int col=0; col<cols; col++)
			{
				int slotId = row*cols + col + 9;
				int x = col*18 + 8;
				int y = row*18 + 84;
				this.addSlot(new Slot(playerInventory, slotId, x, y));
			}
		}
		
		// player hotbar
		for (int i=0; i<9; i++)
		{
			this.addSlot(new Slot(playerInventory, i, i*18 + 8, 142));
		}
	}

	@Override
	public boolean stillValid(Player player)
	{
		return this.validator.test(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotClicked)
	{
		ItemStack copyStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotClicked);
		if (slot != null && slot.hasItem())
		{
			ItemStack stackInSlot = slot.getItem();
			copyStack = stackInSlot.copy();
			if (slotClicked < INVENTORY_SIZE)
			{
				if (!this.moveItemStackTo(stackInSlot, INVENTORY_SIZE, this.slots.size(), true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if (!this.moveItemStackTo(stackInSlot, 0, INVENTORY_SIZE, false))
			{
				return ItemStack.EMPTY;
			}

			if (stackInSlot.isEmpty())
			{
				slot.setByPlayer(ItemStack.EMPTY);
			}
			else
			{
				slot.setChanged();
			}
		}

		return copyStack;
	}
	
	class MultiFilterSlot extends SlotItemHandler
	{
		private final SetItemHandler setItemHandler;

		public MultiFilterSlot(SetItemHandler setItemHandler, int index, int xPosition, int yPosition)
		{
			super(setItemHandler, index, xPosition, yPosition);
			this.setItemHandler = setItemHandler;
		}

		@Override
		public boolean mayPlace(@NotNull ItemStack stack)
		{
			return !this.setItemHandler.getSet().contains(stack.getItem());
		}
		
	}
}
