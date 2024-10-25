package net.commoble.morered.transportation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;

public class LoaderMenu extends AbstractContainerMenu
{
	public final Player player;
	public final BlockPos pos;
	public final Slot loaderSlot;
	
	public LoaderMenu(int id, Inventory playerInventory)
	{
		this(id, playerInventory, BlockPos.ZERO);
	}

	public LoaderMenu(int id, Inventory playerInventory, BlockPos pos)
	{
		super(MoreRed.get().loaderMenu.get(), id);
		this.player = playerInventory.player;
		this.pos = pos;

		// add input slot
		this.loaderSlot = this.addSlot(new LoaderSlot(this, 0, 80, 35));

		// add player inventory
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
		return AbstractContainerMenu.stillValid(ContainerLevelAccess.create(playerIn.level(), this.pos), playerIn, MoreRed.get().loaderBlock.get());
	}

	/**
	 * Handle when the stack in slot {@code index} is shift-clicked. Normally this
	 * moves the stack between the player inventory and the other inventory(s).
	 */
	@Override
	public ItemStack quickMoveStack(Player playerIn, int index)
	{
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem())
		{
			ItemStack stackInSlot = slot.getItem();

			if (!stackInSlot.isEmpty())
			{
				slot.set(ItemStack.EMPTY);
				slot.setChanged();
				this.loaderSlot.set(stackInSlot.copy());
			}
		}

		return ItemStack.EMPTY;
	}
}
