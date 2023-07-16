package commoble.morered.soldering;

import com.google.gson.JsonObject;

import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class SolderingRecipeSerializer extends ShapelessRecipe.Serializer
{
	@Override
	public ShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json)
	{
		ShapelessRecipe baseRecipe = super.fromJson(recipeId, json);
		return new SolderingRecipe(baseRecipe);
	}

	@Override
	public ShapelessRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer)
	{
		ShapelessRecipe baseRecipe = super.fromNetwork(recipeId, buffer);
		return new SolderingRecipe(baseRecipe);
	}
}
