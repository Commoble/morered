package com.github.commoble.morered.gatecrafting_plinth;

import com.github.commoble.morered.RecipeRegistrar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.CraftingResultSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * We have to extend CraftingResultSlot because it doesn't use the recipe type we want to be using
 **/
public class GatecraftingResultSlot extends CraftingResultSlot
{

	public final CraftingInventory craftingInventory;
	public final PlayerEntity player;

	public GatecraftingResultSlot(PlayerEntity player, CraftingInventory craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition)
	{
		super(player, craftingInventory, inventoryIn, slotIndex, xPosition, yPosition);
		this.craftingInventory = craftingInventory;
		this.player = player;
	}

	// from CraftingResultSlot
	@Override
	public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack)
	{
		this.onCrafting(stack);
		net.minecraftforge.common.ForgeHooks.setCraftingPlayer(thePlayer);
		NonNullList<ItemStack> nonnulllist = thePlayer.world.getRecipeManager().getRecipeNonNull(RecipeRegistrar.GATECRAFTING_RECIPE_TYPE, this.craftingInventory, thePlayer.world);
		net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
		for (int i = 0; i < nonnulllist.size(); ++i)
		{
			ItemStack itemstack = this.craftingInventory.getStackInSlot(i);
			ItemStack itemstack1 = nonnulllist.get(i);
			if (!itemstack.isEmpty())
			{
				this.craftingInventory.decrStackSize(i, 1);
				itemstack = this.craftingInventory.getStackInSlot(i);
			}

			if (!itemstack1.isEmpty())
			{
				if (itemstack.isEmpty())
				{
					this.craftingInventory.setInventorySlotContents(i, itemstack1);
				}
				else if (ItemStack.areItemsEqual(itemstack, itemstack1) && ItemStack.areItemStackTagsEqual(itemstack, itemstack1))
				{
					itemstack1.grow(itemstack.getCount());
					this.craftingInventory.setInventorySlotContents(i, itemstack1);
				}
				else if (!this.player.inventory.addItemStackToInventory(itemstack1))
				{
					this.player.dropItem(itemstack1, false);
				}
			}
		}

		return stack;
	}

}
