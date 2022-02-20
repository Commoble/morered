package commoble.morered;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import commoble.morered.gatecrafting_plinth.GatecraftingRecipe;
import commoble.morered.gatecrafting_plinth.GatecraftingRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistrar {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MoreRed.MODID);

    public static final RecipeType<GatecraftingRecipe> GATECRAFTING_RECIPE_TYPE = RecipeType.register("morered" +
            ":gatecrafting");

    public static final RegistryObject<GatecraftingRecipeSerializer> GATECRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register(ObjectNames.GATECRAFTING_RECIPE, GatecraftingRecipeSerializer::new);


    public static List<Recipe<CraftingContainer>> getAllGatecraftingRecipes(RecipeManager manager) {
        return manager.getAllRecipesFor(GATECRAFTING_RECIPE_TYPE)
                .stream()
                .sorted(Comparator.comparing(r -> r.getResultItem().getDescriptionId()))
                .collect(Collectors.toList());
//		Map<ResourceLocation, Recipe<CraftingContainer>> map =
//				(Map)manager.recipes.getOrDefault(GATECRAFTING_RECIPE_TYPE, Collections.emptyMap());
//
//			return map.entrySet().stream()
//			.map(Entry::getValue)
//			.sorted(Comparator.comparing(recipe -> recipe.getResultItem().getDescriptionId()))
//			.collect(Collectors.toList());
    }

    public static Optional<Recipe<CraftingContainer>> getGatecraftingRecipe(RecipeManager manager,
                                                                            ResourceLocation id) {
//		@SuppressWarnings("unchecked")
//		Map<ResourceLocation, Recipe<CraftingContainer>> map =
//				(Map)manager.recipes.getOrDefault(GATECRAFTING_RECIPE_TYPE, Collections.emptyMap());
//		manager.getAllRecipesFor(GATECRAFTING_RECIPE_TYPE).stream().findFirst()
//		manager.getRecipeIds().filter(r -> r.equals(id)).findFirst().map(r -> r.);
//		return Optional.ofNullable(map.get(id));
        return (Optional<Recipe<CraftingContainer>>) manager.byKey(id);
    }

}
