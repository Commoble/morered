package net.commoble.morered.mechanisms;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.transportation.ExtractOnlyItemStacksResourceHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class StonemillMenu extends AbstractContainerMenu
{
	public static final int OUTPUT_SLOT_ID = 0;
	public static final int FIRST_PLAYER_INVENTORY_SLOT_ID = OUTPUT_SLOT_ID + 1;
	public static final int PLAYER_INVENTORY_SLOT_ROWS = 4;
	public static final int PLAYER_INVENTORY_SLOT_COLUMNS = 9;
	public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_SLOT_ROWS * PLAYER_INVENTORY_SLOT_COLUMNS;
	public static final int FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID = FIRST_PLAYER_INVENTORY_SLOT_ID + (PLAYER_INVENTORY_SLOT_ROWS-1) * PLAYER_INVENTORY_SLOT_COLUMNS;
	
	private final ContainerLevelAccess positionInWorld;
	
	public static StonemillMenu clientMenu(int id, Inventory playerInventory)
	{
		ItemStacksResourceHandler handler = new ExtractOnlyItemStacksResourceHandler(1);
		return new StonemillMenu(MoreRed.STONEMILL_MENU.get(), playerInventory, id , handler, handler::set, ContainerLevelAccess.NULL);
	}
	
	public static MenuProvider serverMenuProvider(GenericBlockEntity be)
	{
		Level level = be.getLevel();
		BlockPos pos = be.getBlockPos(); 
		@Nullable ItemStacksResourceHandler inventory = be.getInventory(StonemillBlock.INVENTORY);
		ItemStacksResourceHandler handler = inventory == null
			? new ExtractOnlyItemStacksResourceHandler(1)
			: inventory;
		return new SimpleMenuProvider(
			(id, playerInventory, serverPlayer) -> new StonemillMenu(
				MoreRed.STONEMILL_MENU.get(),
				playerInventory,
				id,
				handler,
				handler::set,
				ContainerLevelAccess.create(level, pos)
			),
			Component.translatable(be.getBlockState().getBlock().getDescriptionId()));
	}

	protected StonemillMenu(MenuType<?> type, Inventory playerInventory, int id, ResourceHandler<ItemResource> itemHandler, IndexModifier<ItemResource> indexModifier, ContainerLevelAccess positionInWorld)
	{
		super(type, id);
		this.positionInWorld = positionInWorld;

		// add input slot
		this.addSlot(new ResourceHandlerSlot(itemHandler, indexModifier, 0, 80, 35));

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
	public ItemStack quickMoveStack(Player player, int slotIndex)
	{
		ItemStack copiedStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot != null && slot.hasItem())
		{
			ItemStack stackInSlot = slot.getItem();
			copiedStack = stackInSlot.copy();
			// if the output slot was clicked
			if (slotIndex == OUTPUT_SLOT_ID)
			{
				if (!this.moveItemStackTo(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID, FIRST_PLAYER_INVENTORY_SLOT_ID + PLAYER_INVENTORY_SLOT_COUNT, true))
				{
					return ItemStack.EMPTY;
				}
			}
			// if a player inventory slot was clicked, try to move it from the hotbar to the backpack or vice-versa
			else if (slotIndex >= FIRST_PLAYER_INVENTORY_SLOT_ID)
			{
				// if it was not a hotbar slot, try to merge it into the hotbar first
				if (slotIndex < FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID)
				{
					if (!this.moveItemStackTo(stackInSlot, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID + PLAYER_INVENTORY_SLOT_COLUMNS, false))
					{
						return ItemStack.EMPTY;
					}
				}
				// if it was a hotbar slot, try to merge it to the player's backpack
				else if (slotIndex >= FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID
					&& slotIndex < FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID + PLAYER_INVENTORY_SLOT_COLUMNS
					&& !this.moveItemStackTo(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID, false))
				{
					return ItemStack.EMPTY;
				}
			}

			if (stackInSlot.isEmpty())
			{
				slot.set(ItemStack.EMPTY);
			}
			else
			{
				slot.setChanged();
			}

			if (stackInSlot.getCount() == copiedStack.getCount())
			{
				return ItemStack.EMPTY;
			}

			slot.onTake(player, stackInSlot);
		}

		return copiedStack;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return stillValid(this.positionInWorld, player, MoreRed.STONEMILL_BLOCK.get());
	}

}
