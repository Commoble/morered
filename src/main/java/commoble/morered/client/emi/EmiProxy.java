package commoble.morered.client.emi;

import commoble.morered.MoreRed;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

@EmiEntrypoint
public class EmiProxy implements EmiPlugin
{
	public static final EmiStack SOLDERING_ICON = EmiStack.of(MoreRed.get().solderingTableBlock.get());
	public static final EmiRecipeCategory SOLDERING_CATEGORY = new EmiRecipeCategory(MoreRed.get().solderingSerializer.getId(), SOLDERING_ICON);

	@Override
	public void register(EmiRegistry registry)
	{
		registry.addCategory(SOLDERING_CATEGORY);
		registry.addWorkstation(SOLDERING_CATEGORY, SOLDERING_ICON);
		
		for (var recipe : registry.getRecipeManager().getAllRecipesFor(MoreRed.get().solderingRecipeType.get()))
		{
			registry.addRecipe(SolderingEmiRecipe.create(recipe));
		}
	}
}
