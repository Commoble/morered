package com.github.commoble.morered.gatecrafting_plinth;

import com.google.gson.JsonObject;

import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class GatecraftingRecipeSerializer extends ShapelessRecipe.Serializer
{
	@Override
	public ShapelessRecipe read(ResourceLocation recipeId, JsonObject json)
	{
		ShapelessRecipe baseRecipe = super.read(recipeId, json);
		return new GatecraftingRecipe(baseRecipe);
	}

	@Override
	public ShapelessRecipe read(ResourceLocation recipeId, PacketBuffer buffer)
	{
		ShapelessRecipe baseRecipe = super.read(recipeId, buffer);
		return new GatecraftingRecipe(baseRecipe);
	}
}
