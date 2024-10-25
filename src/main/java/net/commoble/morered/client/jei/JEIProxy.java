package net.commoble.morered.client.jei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.commoble.morered.MoreRed;
import net.commoble.morered.client.ClientProxy;
import net.commoble.morered.soldering.SolderingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final ResourceLocation ID = MoreRed.id(MoreRed.MODID);
	
	@Nullable
	private SolderingCategory solderingCategory;

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
		
		registration.addRecipes(SolderingCategory.TYPE, getSolderingRecipes().stream().map(RecipeHolder::value).toList());
	}

	/**
	 * Register recipe catalysts. Recipe Catalysts are ingredients that are needed
	 * in order to craft other things. Vanilla examples of Recipe Catalysts are the
	 * Crafting Table and Furnace.
	 */
	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
	{
		registration.addRecipeCatalyst(new ItemStack(MoreRed.get().solderingTableBlock.get()), SolderingCategory.TYPE);
	}

	public static List<RecipeHolder<SolderingRecipe>> getSolderingRecipes()
	{
		@SuppressWarnings("resource")
		ClientLevel clientLevel = Minecraft.getInstance().level;

		if (clientLevel != null)
		{
			RecipeManager manager = clientLevel.getRecipeManager();
			return ClientProxy.getAllSolderingRecipes(manager, clientLevel.registryAccess());
		}
		else
		{
			return ImmutableList.of();
		}
	}

}
