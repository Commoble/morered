package commoble.morered.client.jei;

// TODO fix JEI
public abstract class GatecraftingCategory //implements IRecipeCategory<GatecraftingRecipe>
{
//	public static final ResourceLocation ID = MoreRed.getModRL(ObjectNames.GATECRAFTING_RECIPE);
//	public static final ResourceLocation JEI_RECIPE_TEXTURE = new ResourceLocation(ModIds.JEI_ID, "textures/gui/gui_vanilla.png");
//	
//	private final IDrawable background;
//	private final IDrawable icon;
//	private final String localizedName;
//	
//	public GatecraftingCategory(IGuiHelper guiHelper)
//	{
//		this.icon = guiHelper.createDrawableIngredient(new ItemStack(ItemRegistrar.GATECRAFTING_PLINTH.get()));
//		this.background = guiHelper.createDrawable(JEI_RECIPE_TEXTURE, 0, 60, 116, 54);
//		this.localizedName = I18n.get("gui.morered.category.gatecrafting");
//	}
//
//	@Override
//	public ResourceLocation getUid()
//	{
//		return ID;
//	}
//
//	@Override
//	public Class<? extends GatecraftingRecipe> getRecipeClass()
//	{
//		return GatecraftingRecipe.class;
//	}
//
//	@Override
//	public String getTitle()
//	{
//		return this.localizedName;
//	}
//
//	@Override
//	public IDrawable getBackground()
//	{
//		
//		return this.background;
//	}
//
//	@Override
//	public IDrawable getIcon()
//	{
//		return this.icon;
//	}
//
//	@Override
//	public void setIngredients(GatecraftingRecipe recipe, IIngredients ingredients)
//	{
//		// currently, we only have JEI support for recipes with at most 9 items
//		// JEI doesn't seem to allow for stretchable recipe boxes?
//		// we could do it by cycling through the ingredients over time. Hmm.
//		List<Ingredient> inputs = recipe.getIngredients();
//		int maxItems = Math.min(inputs.size(), 9);
//		ingredients.setInputIngredients(recipe.getIngredients().subList(0, maxItems));
//		ingredients.setOutput(VanillaTypes.ITEM, recipe.getResultItem());
//	}
//
//	@Override
//	public void setRecipe(IRecipeLayout recipeLayout, GatecraftingRecipe recipe, IIngredients ingredients)
//	{
//		// TODO add JEI support for gatecrafting recipes with more than 9 ingredients
//		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
//		
//		// output slot
//		guiItemStacks.init(0, false, 94, 18);
//		
//		// input slots
//		for (int row=0; row<3; row++)
//		{
//			for (int column=0; column<3; column++)
//			{
//				int inputID = row*3 + column + 1;
//				guiItemStacks.init(inputID, true, column*18, row*18);
//			}
//		}
//		
//		guiItemStacks.set(ingredients);
//	}

}
