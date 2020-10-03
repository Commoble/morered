package commoble.morered.gatecrafting_plinth;

import java.util.Optional;

import commoble.morered.BlockRegistrar;
import commoble.morered.ContainerRegistrar;
import commoble.morered.RecipeRegistrar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class GatecraftingContainer extends Container
{
	public static final int OUTPUT_SLOT_ID = 0;
	public static final int FIRST_PLAYER_INVENTORY_SLOT_ID = OUTPUT_SLOT_ID + 1;
	public static final int PLAYER_INVENTORY_SLOT_ROWS = 4;
	public static final int PLAYER_INVENTORY_SLOT_COLUMNS = 9;
	public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_SLOT_ROWS * PLAYER_INVENTORY_SLOT_COLUMNS;
	public static final int FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID = FIRST_PLAYER_INVENTORY_SLOT_ID + (PLAYER_INVENTORY_SLOT_ROWS-1) * PLAYER_INVENTORY_SLOT_COLUMNS;
	
	/** The player that opened the container **/
	private final PlayerEntity player;
	/** This is based on the position of the block the container was opened from (or the position of the player if no block was involved) **/
	private final IWorldPosCallable positionInWorld;
	public final CraftResultInventory craftResult = new CraftResultInventory();
	
	public Optional<IRecipe<CraftingInventory>> currentRecipe = Optional.empty();
	
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
		this.positionInWorld = IWorldPosCallable.of(this.player.world, pos);
		
		// crafting output slot // apparently it's helpful to do this first
//		this.addSlot(new GatecraftingResultSlot(this, playerInventory.player, this.craftingInventory, this.craftResult, OUTPUT_SLOT_ID, inputOffsetX + 94, inputOffsetY + slotHeight));
		this.addSlot(new GatecraftingResultSlot(this, this.craftResult, OUTPUT_SLOT_ID, 220,38));
		
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
		return isWithinUsableDistance(this.positionInWorld, playerIn, BlockRegistrar.GATECRAFTING_PLINTH.get());
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
	
	public void onPlayerChoseRecipe(ResourceLocation recipeID)
	{
		this.attemptRecipeAssembly(RecipeRegistrar.getGatecraftingRecipe(this.player.world.getRecipeManager(), recipeID));
	}
	
	/**
	 * Tries to match the player's current inventory to the given gatecrafting recipe,
	 * returns false if it cannot or the recipe is not a gatecrafting recipe.
	 * @param recipe
	 * @return the recipe if it was successful, the empty optional otherwise
	 */
	public void attemptRecipeAssembly(Optional<IRecipe<CraftingInventory>> recipeHolder)
	{
		Optional<IRecipe<CraftingInventory>> filteredRecipe = recipeHolder.filter(recipe ->
			recipe.getType() == RecipeRegistrar.GATECRAFTING_RECIPE_TYPE && GatecraftingRecipe.doesPlayerHaveIngredients(this.player.inventory, recipe)
		);
		this.updateRecipeAndResult(filteredRecipe);
	}
	
	public void updateRecipeAndResult(Optional<IRecipe<CraftingInventory>> recipeHolder)
	{
		this.currentRecipe = recipeHolder;
		this.craftResult.setInventorySlotContents(0, recipeHolder.map(recipe -> recipe.getRecipeOutput().copy()).orElse(ItemStack.EMPTY));
	}
}
