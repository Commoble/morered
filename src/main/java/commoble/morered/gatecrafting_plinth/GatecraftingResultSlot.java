package commoble.morered.gatecrafting_plinth;

import java.util.Optional;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;

public class GatecraftingResultSlot extends Slot
{
	private final GatecraftingMenu container;

	public GatecraftingResultSlot(GatecraftingMenu container, Container resultInventory, int index, int xPosition, int yPosition)
	{
		super(resultInventory, index, xPosition, yPosition);
		this.container = container;
	}

	/**
	 * Check if the stack is allowed to be placed in this slot, used for armor slots
	 * as well as furnace fuel.
	 */
	@Override
	public boolean mayPlace(ItemStack stack)
	{
		return false;
	}

	@Override
	public void onTake(Player player, ItemStack stack)
	{
		super.onTake(player, stack);
		// ingredients have already been verified by canTakeStack, we can decrement them now
		this.container.currentRecipe.ifPresent(recipe -> this.removeIngredients(player.getInventory(), recipe));
		// set the stack and the container's current recipe accordingly
		this.verifyRecipeAfterCrafting(player.getInventory(), this.container.currentRecipe);
	}

	/**
	 * Return whether this slot's stack can be taken from this slot.
	 */
	@Override
	public boolean mayPickup(Player player)
	{
		return this.container.currentRecipe.map(recipe -> GatecraftingRecipe.doesPlayerHaveIngredients(player.getInventory(), recipe))
			.orElse(false);
	}
	
	public void removeIngredients(Inventory playerInventory, Recipe<CraftingContainer> recipe)
	{
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		for(Ingredient ingredient : ingredients)
		{
			int remainingItemsToRemove = ingredient.getItems()[0].getCount();
			int playerSlots = playerInventory.getContainerSize();
			for (int playerSlot=0; playerSlot<playerSlots && remainingItemsToRemove > 0; playerSlot++)
			{
				ItemStack stackInSlot = playerInventory.getItem(playerSlot);
				if (ingredient.test(stackInSlot))
				{
					int decrementAmount = Math.min(remainingItemsToRemove, stackInSlot.getCount());
					remainingItemsToRemove -= decrementAmount;
					playerInventory.removeItem(playerSlot, decrementAmount);
				}
			}
		}
	}
	
	public void verifyRecipeAfterCrafting(Inventory playerInventory, Optional<Recipe<CraftingContainer>> recipeHolder)
	{
		Optional<Recipe<CraftingContainer>> remainingRecipe = recipeHolder.filter(recipe -> GatecraftingRecipe.doesPlayerHaveIngredients(playerInventory, recipe));
		this.set(remainingRecipe.map(recipe -> recipe.getResultItem().copy()).orElse(ItemStack.EMPTY));
		this.container.currentRecipe = remainingRecipe;
	}
}
