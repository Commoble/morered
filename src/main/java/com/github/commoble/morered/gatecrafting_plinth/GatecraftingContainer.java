package com.github.commoble.morered.gatecrafting_plinth;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.github.commoble.morered.BlockRegistrar;
import com.github.commoble.morered.ContainerRegistrar;
import com.github.commoble.morered.RecipeRegistrar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GatecraftingContainer extends Container
{
	public static final int INPUT_SLOT_ROWS = 3;
	public static final int INPUT_SLOT_COLUMNS = 3;
	public static final int OUTPUT_SLOT_ID = 0;
	public static final int INPUT_SLOT_COUNT = INPUT_SLOT_ROWS*INPUT_SLOT_COLUMNS;
	public static final int FIRST_PLAYER_INVENTORY_SLOT_ID = INPUT_SLOT_COUNT;
	public static final int PLAYER_INVENTORY_SLOT_ROWS = 4;
	public static final int PLAYER_INVENTORY_SLOT_COLUMNS = 9;
	public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_SLOT_ROWS * PLAYER_INVENTORY_SLOT_COLUMNS;
	public static final int FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID = FIRST_PLAYER_INVENTORY_SLOT_ID + (PLAYER_INVENTORY_SLOT_ROWS-1) * PLAYER_INVENTORY_SLOT_COLUMNS;
	
	/** The player that opened the container **/
	private final PlayerEntity player;
	/** This is based on the position of the block the container was opened from (or the position of the player if no block was involved) **/
	private final IWorldPosCallable usableDistanceTest;
	
	private final CraftingInventory craftingInventory= new CraftingInventory(this, 3, 3);
	private final CraftResultInventory craftResult = new CraftResultInventory();
	
	public static GatecraftingContainer getClientContainer(int id, PlayerInventory playerInventory)
	{
		// init client inventory with dummy slots
		return new GatecraftingContainer(id, playerInventory, BlockPos.ZERO);
	}

	public static IContainerProvider getServerContainerProvider(BlockPos pos)
	{
		return (id, playerInventory, theServerPlayer) -> new GatecraftingContainer(id, playerInventory, pos);
	}

	protected GatecraftingContainer(int id, PlayerInventory playerInventory, BlockPos pos)
	{
		super(ContainerRegistrar.GATECRAFTING.get(), id);
		this.player = playerInventory.player;
		this.usableDistanceTest = IWorldPosCallable.of(this.player.world, pos);
		World world = playerInventory.player.world;
		
		// add crafting input slots
		int inputOffsetX = 126;
		int inputOffsetY = 17;
		int slotWidth = 18;
		int slotHeight = 18;
		for (int column=0; column<INPUT_SLOT_COLUMNS; column++)
		{
			for (int row=0; row<INPUT_SLOT_ROWS; row++)
			{
				int slotID = row*INPUT_SLOT_COLUMNS + column;
				int guiX = inputOffsetX + column*slotWidth;
				int guiY = inputOffsetY + row*slotHeight;
				this.addSlot(new Slot(this.craftingInventory, slotID, guiX, guiY));
				if (!world.isRemote)
				{
					List<IRecipe<?>> recipes = getAllGatecraftingRecipes(world.getServer().getRecipeManager());
					if (recipes.size() > slotID)
					{
						System.out.println(recipes.get(slotID).getRecipeOutput());
					}
				}
			}
		}
		
		// crafting output slot
		this.addSlot(new GatecraftingResultSlot(playerInventory.player, this.craftingInventory, this.craftResult, OUTPUT_SLOT_ID, inputOffsetX + 94, inputOffsetY + slotHeight));
		
		// add player inventory
		for (int column = 0; column < 3; ++column)
		{
			for (int row = 0; row < 9; ++row)
			{
	            this.addSlot(new Slot(playerInventory, row + column * 9 + 9, 108 + row * 18, 84 + column * 18));
			}
		}

		// player inventory hotbar slots
		for (int i = 0; i < 9; ++i)
		{
			this.addSlot(new Slot(playerInventory, i, 108 + i * 18, 142));
		}
	}

	/**
	 * Determines whether supplied player can use this container
	 */
	@Override
	public boolean canInteractWith(PlayerEntity playerIn)
	{
		return isWithinUsableDistance(this.usableDistanceTest, playerIn, BlockRegistrar.GATECRAFTING_PLINTH.get());
	}

	/**
	 * Callback for when the crafting matrix is changed.
	 */
	@Override
	public void onCraftMatrixChanged(IInventory inventoryIn)
	{
		// this.merchantInventory.resetRecipeAndSlots();
		System.out.println("boop");
		super.onCraftMatrixChanged(inventoryIn);
	}

	/**
	 * Called to determine if the current slot is valid for the stack merging
	 * (double-click) code. The stack passed in is null for the initial slot that
	 * was double-clicked.
	 */
	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slotIn)
	{
		return false;
	}

	/**
	 * Handle when the stack in slot {@code index} is shift-clicked. Normally this
	 * moves the stack between the player inventory and the other inventory(s).
	 */
	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int slotIndex)
	{
		ItemStack copiedStack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(slotIndex);
		if (slot != null && slot.getHasStack())
		{
			ItemStack stackInSlot = slot.getStack();
			copiedStack = stackInSlot.copy();
			// if the output slot was clicked
			if (slotIndex == OUTPUT_SLOT_ID)
			{
				if (!this.mergeItemStack(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID, FIRST_PLAYER_INVENTORY_SLOT_ID + PLAYER_INVENTORY_SLOT_COUNT, true))
				{
					return ItemStack.EMPTY;
				}

				slot.onSlotChange(stackInSlot, copiedStack);
//				this.playMerchantYesSound();
			}
			// if a player inventory slot was clicked, try to move it from the hotbar to the backpack or vice-versa
			else if (slotIndex >= FIRST_PLAYER_INVENTORY_SLOT_ID)
			{
				// if it was not a hotbar slot, try to merge it into the hotbar first
				if (slotIndex < FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID)
				{
					if (!this.mergeItemStack(stackInSlot, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID + PLAYER_INVENTORY_SLOT_COLUMNS, false))
					{
						return ItemStack.EMPTY;
					}
				}
				// if it was a hotbar slot, try to merge it to the player's backpack
				else if (slotIndex >= FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID
					&& slotIndex < FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID + PLAYER_INVENTORY_SLOT_COLUMNS
					&& !this.mergeItemStack(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID, false))
				{
					return ItemStack.EMPTY;
				}
			}
			// otherwise, an input slot was clicked -- try to move it to the player's inventory
			else if (!this.mergeItemStack(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID, FIRST_PLAYER_INVENTORY_SLOT_ID + PLAYER_INVENTORY_SLOT_COUNT, false))
			{
				return ItemStack.EMPTY;
			}

			if (stackInSlot.isEmpty())
			{
				slot.putStack(ItemStack.EMPTY);
			}
			else
			{
				slot.onSlotChanged();
			}

			if (stackInSlot.getCount() == copiedStack.getCount())
			{
				return ItemStack.EMPTY;
			}

			slot.onTake(playerIn, stackInSlot);
		}

		return copiedStack;
	}

	/**
	 * Called when the container is closed.
	 */
	@Override
	public void onContainerClosed(PlayerEntity player)
	{
		super.onContainerClosed(player);
		if (player != null && !player.world.isRemote)
		{
			// if the player is not available, dump the any items in the container inventory on the ground
			if (!player.isAlive() || player instanceof ServerPlayerEntity && ((ServerPlayerEntity) player).hasDisconnected())
			{
				for (int i=0; i<9; i++)
				{
					ItemStack itemstack = this.craftingInventory.removeStackFromSlot(i);
					if (!itemstack.isEmpty())
					{
						player.dropItem(itemstack, false);
					}
				}
			}
			else // if the player is available, attempt to put the items back in the player inventory
			{
				for (int i=0; i<9; i++)
				{
					player.inventory.placeItemBackInInventory(player.world, this.craftingInventory.removeStackFromSlot(i));
				}
			}

		}
	}

	public static List<IRecipe<?>> getAllGatecraftingRecipes(RecipeManager manager)
	{
		return manager.recipes.getOrDefault(RecipeRegistrar.GATECRAFTING_RECIPE_TYPE, Collections.emptyMap()).entrySet().stream()
			.map(Entry::getValue)
			.sorted(Comparator.comparing(recipe -> recipe.getRecipeOutput().getTranslationKey()))
			.collect(Collectors.toList());
	}
}
