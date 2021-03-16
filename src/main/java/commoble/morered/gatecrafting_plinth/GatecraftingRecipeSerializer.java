package commoble.morered.gatecrafting_plinth;

import com.google.gson.JsonObject;

import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class GatecraftingRecipeSerializer extends ShapelessRecipe.Serializer
{
	@Override
	public ShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json)
	{
		ShapelessRecipe baseRecipe = super.fromJson(recipeId, json);
		return new GatecraftingRecipe(baseRecipe);
	}

	@Override
	public ShapelessRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer)
	{
		ShapelessRecipe baseRecipe = super.fromNetwork(recipeId, buffer);
		return new GatecraftingRecipe(baseRecipe);
	}
}
