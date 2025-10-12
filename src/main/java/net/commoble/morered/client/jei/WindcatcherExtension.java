package net.commoble.morered.client.jei;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.commoble.morered.MoreRed;
import net.commoble.morered.mechanisms.WindcatcherColors;
import net.commoble.morered.mechanisms.WindcatcherRecipe;
import net.commoble.morered.mechanisms.WindcatcherRecipe.XY;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public enum WindcatcherExtension implements ICraftingCategoryExtension<WindcatcherRecipe>
{
	INSTANCE;
	
	@Override
	public List<SlotDisplay> getIngredients(RecipeHolder<WindcatcherRecipe> recipeHolder)
	{
		List<RecipeDisplay> displays = recipeHolder.value().display();
		return displays.isEmpty() || !(displays.getFirst() instanceof ShapedCraftingRecipeDisplay shapedDisplay)
			? List.of()
			: shapedDisplay.ingredients();
	}

	@Override
	public int getWidth(RecipeHolder<WindcatcherRecipe> recipeHolder)
	{
		return recipeHolder.value().getWidth();
	}

	@Override
	public int getHeight(RecipeHolder<WindcatcherRecipe> recipeHolder)
	{
		return recipeHolder.value().getHeight();
	}

	@Override
	public void onDisplayedIngredientsUpdate(RecipeHolder<WindcatcherRecipe> recipeHolder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses)
	{
		int size = recipeSlots.size();
		if (size < 2)
			return;
		WindcatcherRecipe recipe = recipeHolder.value();
		IRecipeSlotDrawable outputSlot = recipeSlots.get(0); // jei adds the output first
		outputSlot.getDisplayedItemStack().ifPresent(outputStack -> {
			Set<XY> relevantPositions = Set.of(recipe.north(), recipe.south(), recipe.west(), recipe.east());
			Map<XY, DyeColor> colorMap = new HashMap<>();
			for (int i=1; i<size; i++)
			{
				// jei adds ingredients left-to-right, in rows top-to-bottom
				// so first three are in the top row
				int x = (i-1) % 3;
				int y = (i-1) / 3;
				XY xy = new XY(x,y);
				if (relevantPositions.contains(xy))
				{
					recipeSlots.get(i).getDisplayedItemStack().ifPresent(stack -> {
						colorMap.put(xy, recipe.getColor(stack));
					});
				}
			}
			WindcatcherColors windcatcherColors = new WindcatcherColors(
				colorMap.getOrDefault(recipe.north(), recipe.defaultColor()),
				colorMap.getOrDefault(recipe.south(), recipe.defaultColor()),
				colorMap.getOrDefault(recipe.west(), recipe.defaultColor()),
				colorMap.getOrDefault(recipe.east(), recipe.defaultColor()));
			ItemStack newOutputStack = outputStack.copy();
			newOutputStack.set(MoreRed.WINDCATCHER_COLORS_DATA_COMPONENT.get(), windcatcherColors);
			outputSlot.createDisplayOverrides().add(newOutputStack);
		});
	}
}
