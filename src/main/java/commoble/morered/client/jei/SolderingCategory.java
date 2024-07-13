package commoble.morered.client.jei;

import java.util.List;

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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class SolderingCategory implements IRecipeCategory<SolderingRecipe>
{
	public static final RecipeType<SolderingRecipe> TYPE = RecipeType.create(MoreRed.MODID, ObjectNames.SOLDERING_RECIPE, SolderingRecipe.class);
	public static final ResourceLocation JEI_RECIPE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ModIds.JEI_ID, "textures/jei/gui/gui_vanilla.png");
	public static final String TITLE = "gui.morered.category.soldering";
	
	private final IDrawable background;
	private final IDrawable icon;
	
	public SolderingCategory(IGuiHelper guiHelper)
	{
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(MoreRed.get().solderingTableBlock.get()));
		this.background = guiHelper.createDrawable(JEI_RECIPE_TEXTURE, 0, 60, 116, 54);
	}
	
	

	@Override
	public RecipeType<SolderingRecipe> getRecipeType()
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

	@SuppressWarnings("resource")
	@Override
	public void setRecipe(IRecipeLayoutBuilder recipeLayout, SolderingRecipe recipe, IFocusGroup focuses)
	{
		recipeLayout.setShapeless();
		
		// output slot
		recipeLayout.addSlot(RecipeIngredientRole.OUTPUT, 95, 19)
			.addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
		
		// input slots
		List<SizedIngredient> ingredients = recipe.ingredients();
		int ingredientCount = ingredients.size();
		for (int row=0; row<3; row++)
		{
			for (int column=0; column<3; column++)
			{
				int inputID = row*3 + column;
				var slot = recipeLayout.addSlot(RecipeIngredientRole.INPUT, column*18 + 1, row*18 + 1);
				if (inputID < ingredientCount)
				{
					slot.addItemStacks(List.of(ingredients.get(inputID).getItems()));
				}
			}
		}
	}

}
