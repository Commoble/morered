package net.commoble.morered.client.emi;

//public record SolderingEmiRecipe(Identifier id, List<EmiIngredient> inputs, List<EmiStack> outputs) implements EmiRecipe
public record SolderingEmiRecipe()
{
//	public static SolderingEmiRecipe create(RecipeHolder<SolderingRecipe> recipe)
//	{
//		return new SolderingEmiRecipe(recipe.id(),
//			recipe.value().ingredients().stream().map(sizedIngredient -> EmiIngredient.of(sizedIngredient.ingredient(), sizedIngredient.count())).toList(),
//			List.of(EmiStack.of(recipe.value().result())));
//	}
//
//	@Override
//	public EmiRecipeCategory getCategory()
//	{
//		return EmiProxy.SOLDERING_CATEGORY;
//	}
//
//	@Override
//	public @Nullable Identifier getId()
//	{
//		return id;
//	}
//
//	@Override
//	public List<EmiIngredient> getInputs()
//	{
//		return inputs;
//	}
//
//	@Override
//	public List<EmiStack> getOutputs()
//	{
//		return outputs;
//	}
//
//	@Override
//	public int getDisplayWidth() {
//		return 118;
//	}
//
//	@Override
//	public int getDisplayHeight() {
//		return 54;
//	}
//
//	@Override
//	public void addWidgets(WidgetHolder widgets)
//	{
//		widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
//		widgets.addTexture(EmiTexture.SHAPELESS, 97, 0);
//		int inputCount = this.inputs.size();
//		for (int i = 0; i < 9; i++)
//		{
//			int row = i / 3;
//			int column = i % 3;
//			int inputX = column*18;
//			int inputY = row*18;
//			EmiIngredient input = i < inputCount ? this.inputs.get(i) : EmiStack.of(ItemStack.EMPTY);
//			widgets.addSlot(input, inputX, inputY);
//		}
//		widgets.addSlot(this.outputs.get(0), 92, 14).large(true).recipeContext(this);
//	}
}