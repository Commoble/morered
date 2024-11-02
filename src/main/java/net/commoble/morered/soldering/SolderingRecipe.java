package net.commoble.morered.soldering;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.MoreRed;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public record SolderingRecipe(ItemStack result, List<SizedIngredient> ingredients) implements Recipe<CraftingInput>
{
	public static final MapCodec<SolderingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			ItemStack.CODEC.fieldOf("result").forGetter(SolderingRecipe::result),
			SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(SolderingRecipe::ingredients)
		).apply(builder, SolderingRecipe::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, SolderingRecipe> STREAM_CODEC = StreamCodec.composite(
		ItemStack.STREAM_CODEC, SolderingRecipe::result,
		SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), SolderingRecipe::ingredients,
		SolderingRecipe::new);
	
	// can't deal with RecipeHolder generics
	public record SolderingRecipeHolder(ResourceLocation id, SolderingRecipe recipe) {
		public static final StreamCodec<RegistryFriendlyByteBuf, SolderingRecipeHolder> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, SolderingRecipeHolder::id,
			SolderingRecipe.STREAM_CODEC, SolderingRecipeHolder::recipe,
			SolderingRecipeHolder::new);
	}

	@Override
	public RecipeType<? extends Recipe<CraftingInput>> getType()
	{
		return MoreRed.get().solderingRecipeType.get();
	}

	@Override
	public RecipeSerializer<? extends Recipe<CraftingInput>> getSerializer()
	{
		return MoreRed.get().solderingSerializer.get();
	}
	
	public static boolean doesPlayerHaveIngredients(Inventory playerInventory, @NotNull SolderingRecipe recipe)
	{
		// assumes that the ingredient list doesn't have two of the same ingredients
		// e.g. two stacks of redstone
		List<SizedIngredient> ingredients = recipe.ingredients();
		for (SizedIngredient ingredient : ingredients)
		{
			int remainingItems = ingredient.count();
			int playerSlots = playerInventory.getContainerSize();
			for (int playerSlot = 0; playerSlot < playerSlots && remainingItems > 0; playerSlot++)
			{
				ItemStack stackInSlot = playerInventory.getItem(playerSlot);
				if (ingredient.test(stackInSlot))
				{
					remainingItems -= stackInSlot.getCount();
				}
			}
			if (remainingItems > 0)
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean matches(CraftingInput input, Level level)
	{
		// TODO we handle recipe matching manually but we could theoretically move it here
		return false;
	}

	@Override
	public ItemStack assemble(CraftingInput input, Provider registries)
	{
		return this.result.copy();
	}

	@Override
	public boolean isSpecial()
	{
		return true; // keeps vanilla recipe book from logspamming
	}

	@Override
	public boolean showNotification()
	{
		return false;
	}

	@Override
	public PlacementInfo placementInfo()
	{
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory()
	{
		return RecipeBookCategories.CRAFTING_MISC;
	}
}
