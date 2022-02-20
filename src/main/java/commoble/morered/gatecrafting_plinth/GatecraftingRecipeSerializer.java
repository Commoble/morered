package commoble.morered.gatecrafting_plinth;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class GatecraftingRecipeSerializer extends ShapelessRecipe.Serializer {
    @Override
    public ShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        ShapelessRecipe baseRecipe = super.fromJson(recipeId, json);
        return new GatecraftingRecipe(baseRecipe);
    }

    @Override
    public ShapelessRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        ShapelessRecipe baseRecipe = super.fromNetwork(recipeId, buffer);
        return new GatecraftingRecipe(baseRecipe);
    }
}
