package net.commoble.morered.soldering;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public class SolderingMenu extends AbstractContainerMenu
{
	public static final int OUTPUT_SLOT_ID = 0;
	public static final int FIRST_PLAYER_INVENTORY_SLOT_ID = OUTPUT_SLOT_ID + 1;
	public static final int PLAYER_INVENTORY_SLOT_ROWS = 4;
	public static final int PLAYER_INVENTORY_SLOT_COLUMNS = 9;
	public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_SLOT_ROWS * PLAYER_INVENTORY_SLOT_COLUMNS;
	public static final int FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID = FIRST_PLAYER_INVENTORY_SLOT_ID + (PLAYER_INVENTORY_SLOT_ROWS-1) * PLAYER_INVENTORY_SLOT_COLUMNS;
	
	/** The player that opened the container **/
	private final Player player;
	/** This is based on the position of the block the container was opened from (or the position of the player if no block was involved) **/
	private final ContainerLevelAccess positionInWorld;
	public final ResultContainer craftResult = new ResultContainer();
	
	public @Nullable SolderingRecipe currentRecipe = null;
	
	public static SolderingMenu getClientContainer(int id, Inventory playerInventory)
	{
		// init client inventory with dummy slots
		return new SolderingMenu(id, playerInventory, BlockPos.ZERO);
	}

	public static MenuConstructor getServerContainerProvider(BlockPos pos)
	{
		return (id, playerInventory, theServerPlayer) -> new SolderingMenu(id, playerInventory, pos);
	}

	protected SolderingMenu(int id, Inventory playerInventory, BlockPos pos)
	{
		super(MoreRed.SOLDERING_MENU.get(), id);
		this.player = playerInventory.player;
		this.positionInWorld = ContainerLevelAccess.create(this.player.level(), pos);
		
		// crafting output slot // apparently it's helpful to do this first
		this.addSlot(new SolderingResultSlot(this, this.craftResult, OUTPUT_SLOT_ID, 220,38));
		
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
	public boolean stillValid(Player playerIn)
	{
		return stillValid(this.positionInWorld, playerIn, MoreRed.SOLDERING_TABLE_BLOCK.get());
	}

		/**
		 * Called to determine if the current slot is valid for the stack merging
		 * (double-click) code. The stack passed in is null for the initial slot that
		 * was double-clicked.
		 */
	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn)
	{
		return false;
	}

	/**
	 * Handle when the stack in slot {@code index} is shift-clicked. Normally this
	 * moves the stack between the player inventory and the other inventory(s).
	 */
	@Override
	public ItemStack quickMoveStack(Player playerIn, int slotIndex)
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

				slot.onQuickCraft(stackInSlot, copiedStack);
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

			slot.onTake(playerIn, stackInSlot);
		}

		return copiedStack;
	}
	
	
	public void onPlayerChoseRecipe(ServerPlayer serverPlayer, ResourceKey<Recipe<?>> recipeID)
	{
		var recipe = getSolderingRecipe(serverPlayer.getServer().getRecipeManager(), recipeID);
		if (recipe == null)
			return;
		this.attemptRecipeAssembly(recipe.value());
	}

	public static @Nullable RecipeHolder<SolderingRecipe> getSolderingRecipe(RecipeManager manager, ResourceKey<Recipe<?>> id)
	{
		return manager.byKeyTyped(MoreRed.SOLDERING_RECIPE_TYPE.get(), id);
	}
	
	/**
	 * Attempts to assemble the given recipe and updates crafting result if successful
	 */
	public void attemptRecipeAssembly(@Nullable SolderingRecipe recipe)
	{
		this.currentRecipe = recipe;
		if (recipe == null || !SolderingRecipe.doesPlayerHaveIngredients(this.player.getInventory(), recipe))
		{
			this.craftResult.setItem(0, ItemStack.EMPTY);
		}
		else
		{
			this.craftResult.setItem(0, recipe.result().copy());
		}
	}
}
