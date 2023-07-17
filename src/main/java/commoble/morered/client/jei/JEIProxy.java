package commoble.morered.client.jei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import commoble.morered.MoreRed;
import commoble.morered.soldering.SolderingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final ResourceLocation ID = new ResourceLocation(MoreRed.MODID, MoreRed.MODID);
	
	@Nullable
	private GatecraftingCategory gatecraftingCategory;

	@Override
	public ResourceLocation getPluginUid()
	{
		return ID;
	}

	/**
	 * Register the categories handled by this plugin. These are registered before
	 * recipes so they can be checked for validity.
	 */
	@Override
	public void registerCategories(IRecipeCategoryRegistration registration)
	{
		this.gatecraftingCategory = new GatecraftingCategory(registration.getJeiHelpers().getGuiHelper());
		registration.addRecipeCategories(this.gatecraftingCategory);
	}

	/**
	 * Register modded recipes.
	 */
	@Override
	public void registerRecipes(IRecipeRegistration registration)
	{
		if (this.gatecraftingCategory == null)
		{
			throw new NullPointerException("More Red's Gatecrafting JEI category failed to register! Notify the More Red author for assistance https://github.com/Commoble/morered/issues");
		}
		
		registration.addRecipes(GatecraftingCategory.TYPE, getGatecraftingRecipes());
	}

	/**
	 * Register recipe catalysts. Recipe Catalysts are ingredients that are needed
	 * in order to craft other things. Vanilla examples of Recipe Catalysts are the
	 * Crafting Table and Furnace.
	 */
	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
	{
		registration.addRecipeCatalyst(new ItemStack(MoreRed.get().solderingTableBlock.get()), GatecraftingCategory.TYPE);
	}

	public static List<Recipe<CraftingContainer>> getGatecraftingRecipes()
	{
		@SuppressWarnings("resource")
		ClientLevel clientLevel = Minecraft.getInstance().level;

		if (clientLevel != null)
		{
			RecipeManager manager = clientLevel.getRecipeManager();
			return SolderingRecipe.getAllSolderingRecipes(manager, clientLevel.registryAccess());
		}
		else
		{
			return ImmutableList.of();
		}
	}

}
