package commoble.morered.client.jei;

// TODO fix JEI
//@JeiPlugin
public class JEIProxy // implements IModPlugin
{
//	public static final ResourceLocation ID = new ResourceLocation(MoreRed.MODID, MoreRed.MODID);
//	
//	@Nullable
//	private GatecraftingCategory gatecraftingCategory;
//
//	@Override
//	public ResourceLocation getPluginUid()
//	{
//		return ID;
//	}
//
//	/**
//	 * Register the categories handled by this plugin. These are registered before
//	 * recipes so they can be checked for validity.
//	 */
//	@Override
//	public void registerCategories(IRecipeCategoryRegistration registration)
//	{
//		this.gatecraftingCategory = new GatecraftingCategory(registration.getJeiHelpers().getGuiHelper());
//		registration.addRecipeCategories(this.gatecraftingCategory);
//	}
//
//	/**
//	 * Register modded recipes.
//	 */
//	@Override
//	public void registerRecipes(IRecipeRegistration registration)
//	{
//		if (this.gatecraftingCategory == null)
//		{
//			throw new NullPointerException("More Red's Gatecrafting JEI category failed to register! Notify the More Red author for assistance https://github.com/Commoble/morered/issues");
//		}
//		
//		registration.addRecipes(getGatecraftingRecipes(), GatecraftingCategory.ID);
//	}
//
//	/**
//	 * Register recipe catalysts. Recipe Catalysts are ingredients that are needed
//	 * in order to craft other things. Vanilla examples of Recipe Catalysts are the
//	 * Crafting Table and Furnace.
//	 */
//	@Override
//	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
//	{
//		registration.addRecipeCatalyst(new ItemStack(ItemRegistrar.GATECRAFTING_PLINTH.get()), GatecraftingCategory.ID);
//	}
//
//	public static List<IRecipe<CraftingInventory>> getGatecraftingRecipes()
//	{
//		ClientWorld world = Minecraft.getInstance().level;
//
//		if (world != null)
//		{
//			RecipeManager manager = world.getRecipeManager();
//			return RecipeRegistrar.getAllGatecraftingRecipes(manager);
//		}
//		else
//		{
//			return ImmutableList.of();
//		}
//	}

}
