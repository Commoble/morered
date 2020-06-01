package com.github.commoble.morered.gatecrafting_plinth;

import com.github.commoble.morered.RecipeRegistrar;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.ShapelessRecipe;

/** Wrapper around vanilla shapeless recipes to change the recipe type **/
public class GatecraftingRecipe extends ShapelessRecipe
{
	public GatecraftingRecipe(ShapelessRecipe baseRecipe)
	{
		super(baseRecipe.getId(), baseRecipe.getGroup(), baseRecipe.getRecipeOutput(), baseRecipe.getIngredients());
	}

	@Override
	public IRecipeType<?> getType()
	{
		return RecipeRegistrar.GATECRAFTING_RECIPE_TYPE;
	}

	@Override
	public IRecipeSerializer<?> getSerializer()
	{
		return RecipeRegistrar.GATECRAFTING_SERIALIZER.get();
	}

}
