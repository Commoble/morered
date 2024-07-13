package commoble.morered.soldering;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class SolderingResultSlot extends Slot
{
	private final SolderingMenu container;

	public SolderingResultSlot(SolderingMenu container, Container resultInventory, int index, int xPosition, int yPosition)
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
		if (this.container.currentRecipe != null)
		{
			this.removeIngredients(player.getInventory(), this.container.currentRecipe);
		}
		// set the stack and the container's current recipe accordingly
		this.verifyRecipeAfterCrafting(player.getInventory(), this.container.currentRecipe);
	}

	/**
	 * Return whether this slot's stack can be taken from this slot.
	 */
	@Override
	public boolean mayPickup(Player player)
	{
		return this.container.currentRecipe != null;
	}
	
	public void removeIngredients(Inventory playerInventory, SolderingRecipe recipe)
	{
		List<SizedIngredient> ingredients = recipe.ingredients();
		for(SizedIngredient ingredient : ingredients)
		{
			int remainingItemsToRemove = ingredient.count();
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
	
	public void verifyRecipeAfterCrafting(Inventory playerInventory, @Nullable SolderingRecipe recipe)
	{
		if (recipe != null && SolderingRecipe.doesPlayerHaveIngredients(playerInventory, recipe))
		{
			this.set(recipe.getResultItem(playerInventory.player.level().registryAccess()).copy());
			this.container.currentRecipe = recipe;
		}
		else
		{
			this.set(ItemStack.EMPTY);
			this.container.currentRecipe = null;
		}
	}
}
