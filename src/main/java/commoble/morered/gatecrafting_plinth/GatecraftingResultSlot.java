package commoble.morered.gatecrafting_plinth;

import java.util.Optional;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

public class GatecraftingResultSlot extends Slot
{
	private final GatecraftingContainer container;

	public GatecraftingResultSlot(GatecraftingContainer container, IInventory resultInventory, int index, int xPosition, int yPosition)
	{
		super(resultInventory, index, xPosition, yPosition);
		this.container = container;
	}

	/**
	 * Check if the stack is allowed to be placed in this slot, used for armor slots
	 * as well as furnace fuel.
	 */
	@Override
	public boolean isItemValid(ItemStack stack)
	{
		return false;
	}

	@Override
	public ItemStack onTake(PlayerEntity player, ItemStack stack)
	{
		// ingredients have already been verified by canTakeStack, we can decrement them now
		this.onSlotChanged();
		this.container.currentRecipe.ifPresent(recipe -> this.removeIngredients(player.inventory, recipe));
		// set the stack and the container's current recipe accordingly
		this.verifyRecipeAfterCrafting(player.inventory, this.container.currentRecipe);
		return stack;
	}

	/**
	 * Return whether this slot's stack can be taken from this slot.
	 */
	@Override
	public boolean canTakeStack(PlayerEntity player)
	{
		return this.container.currentRecipe.map(recipe -> GatecraftingRecipe.doesPlayerHaveIngredients(player.inventory, recipe))
			.orElse(false);
	}
	
	public void removeIngredients(PlayerInventory playerInventory, IRecipe<CraftingInventory> recipe)
	{
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		for(Ingredient ingredient : ingredients)
		{
			int remainingItemsToRemove = ingredient.getMatchingStacks()[0].getCount();
			int playerSlots = playerInventory.getSizeInventory();
			for (int playerSlot=0; playerSlot<playerSlots && remainingItemsToRemove > 0; playerSlot++)
			{
				ItemStack stackInSlot = playerInventory.getStackInSlot(playerSlot);
				if (ingredient.test(stackInSlot))
				{
					int decrementAmount = Math.min(remainingItemsToRemove, stackInSlot.getCount());
					remainingItemsToRemove -= decrementAmount;
					playerInventory.decrStackSize(playerSlot, decrementAmount);
				}
			}
		}
	}
	
	public void verifyRecipeAfterCrafting(PlayerInventory playerInventory, Optional<IRecipe<CraftingInventory>> recipeHolder)
	{
		Optional<IRecipe<CraftingInventory>> remainingRecipe = recipeHolder.filter(recipe -> GatecraftingRecipe.doesPlayerHaveIngredients(playerInventory, recipe));
		this.putStack(remainingRecipe.map(recipe -> recipe.getRecipeOutput().copy()).orElse(ItemStack.EMPTY));
		this.container.currentRecipe = remainingRecipe;
	}
}
