package commoble.morered.client.jei;

import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.soldering.SolderingRecipe;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class GatecraftingCategory implements IRecipeCategory<Recipe<CraftingContainer>>
{
	public static final RecipeType<Recipe<CraftingContainer>> TYPE = RecipeType.create(MoreRed.MODID, ObjectNames.SOLDERING_RECIPE, SolderingRecipe.class);
	public static final ResourceLocation JEI_RECIPE_TEXTURE = new ResourceLocation(ModIds.JEI_ID, "textures/gui/gui_vanilla.png");
	public static final String TITLE = "gui.morered.category.gatecrafting";
	
	private final IDrawable background;
	private final IDrawable icon;
	
	public GatecraftingCategory(IGuiHelper guiHelper)
	{
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(MoreRed.get().solderingTableBlock.get()));
		this.background = guiHelper.createDrawable(JEI_RECIPE_TEXTURE, 0, 60, 116, 54);
	}
	
	

	@Override
	public RecipeType<Recipe<CraftingContainer>> getRecipeType()
	{
		return TYPE;
	}

	@Override
	public Component getTitle()
	{
		return Component.translatable(TITLE);
	}

	@Override
	public IDrawable getBackground()
	{
		
		return this.background;
	}

	@Override
	public IDrawable getIcon()
	{
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder recipeLayout, Recipe<CraftingContainer> recipe, IFocusGroup focuses)
	{
//		// TODO add JEI support for gatecrafting recipes with more than 9 ingredients
		recipeLayout.setShapeless();
		
		// output slot
		recipeLayout.addSlot(RecipeIngredientRole.OUTPUT, 94, 18)
			.addItemStack(RecipeUtil.getResultItem(recipe));
		
		// input slots
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		int ingredientCount = ingredients.size();
		for (int row=0; row<3; row++)
		{
			for (int column=0; column<3; column++)
			{
				int inputID = row*3 + column;
				var slot = recipeLayout.addSlot(RecipeIngredientRole.INPUT, column*18, row*18);
				if (inputID < ingredientCount)
				{
					slot.addIngredients(ingredients.get(inputID));
				}
			}
		}
	}

}
