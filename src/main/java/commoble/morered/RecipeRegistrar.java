package commoble.morered;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import commoble.morered.gatecrafting_plinth.GatecraftingRecipe;
import commoble.morered.gatecrafting_plinth.GatecraftingRecipeSerializer;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RecipeRegistrar
{
	public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MoreRed.MODID);
	
	public static final IRecipeType<GatecraftingRecipe> GATECRAFTING_RECIPE_TYPE = IRecipeType.register("morered:gatecrafting");
	
	public static final RegistryObject<GatecraftingRecipeSerializer> GATECRAFTING_SERIALIZER = RECIPE_SERIALIZERS.register(ObjectNames.GATECRAFTING_RECIPE,
		() -> new GatecraftingRecipeSerializer());
	


	public static List<IRecipe<CraftingInventory>> getAllGatecraftingRecipes(RecipeManager manager)
	{
//		return manager.recipes.getOrDefault(RecipeRegistrar.GATECRAFTING_RECIPE_TYPE, Collections.emptyMap()).entrySet().stream()
		Map<ResourceLocation, IRecipe<CraftingInventory>> map = (Map)manager.recipes.getOrDefault(GATECRAFTING_RECIPE_TYPE, Collections.emptyMap());
			return map.entrySet().stream()
			.map(Entry::getValue)
			.sorted(Comparator.comparing(recipe -> recipe.getResultItem().getDescriptionId()))
			.collect(Collectors.toList());
	}

	public static Optional<IRecipe<CraftingInventory>> getGatecraftingRecipe(RecipeManager manager, ResourceLocation id)
	{
		@SuppressWarnings("unchecked")
		Map<ResourceLocation, IRecipe<CraftingInventory>> map = (Map)manager.recipes.getOrDefault(GATECRAFTING_RECIPE_TYPE, Collections.emptyMap());
		return Optional.ofNullable(map.get(id));
	}

}
