package net.commoble.morered.client.jei;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.commoble.morered.MoreRed;
import net.commoble.morered.client.ClientProxy;
import net.commoble.morered.mechanisms.WindcatcherRecipe;
import net.commoble.morered.soldering.SolderingRecipe.SolderingRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final ResourceLocation ID = MoreRed.id(MoreRed.MODID);
	
	private @Nullable SolderingCategory solderingCategory;

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
		this.solderingCategory = new SolderingCategory(registration.getJeiHelpers().getGuiHelper());
		registration.addRecipeCategories(this.solderingCategory);
	}
	
	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration)
	{
		registration.getCraftingCategory().addExtension(WindcatcherRecipe.class, WindcatcherExtension.INSTANCE);
	}

	/**
	 * Register modded recipes.
	 */
	@Override
	public void registerRecipes(IRecipeRegistration registration)
	{
		if (this.solderingCategory == null)
		{
			throw new NullPointerException("More Red's Soldering JEI category failed to register! Notify the More Red author for assistance https://github.com/Commoble/morered/issues");
		}
		
		registration.addRecipes(SolderingCategory.TYPE, ClientProxy.getAllSolderingRecipes().stream().map(SolderingRecipeHolder::recipe).toList());
	}

	/**
	 * Register recipe catalysts. Recipe Catalysts are ingredients that are needed
	 * in order to craft other things. Vanilla examples of Recipe Catalysts are the
	 * Crafting Table and Furnace.
	 */
	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
	{
		registration.addCraftingStation(SolderingCategory.TYPE, new ItemStack(MoreRed.SOLDERING_TABLE_BLOCK.get()));
	}
}
