package com.github.commoble.morered;

import com.github.commoble.morered.gatecrafting_plinth.GatecraftingRecipe;
import com.github.commoble.morered.gatecrafting_plinth.GatecraftingRecipeSerializer;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RecipeRegistrar
{
	public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = new DeferredRegister<>(ForgeRegistries.RECIPE_SERIALIZERS, MoreRed.MODID);
	
	public static final IRecipeType<GatecraftingRecipe> GATECRAFTING_RECIPE_TYPE = IRecipeType.register("morered:gatecrafting");
	
	public static final RegistryObject<GatecraftingRecipeSerializer> GATECRAFTING_SERIALIZER = RECIPE_SERIALIZERS.register(ObjectNames.GATECRAFTING_RECIPE,
		() -> new GatecraftingRecipeSerializer());
}
