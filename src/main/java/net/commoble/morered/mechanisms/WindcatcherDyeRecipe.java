package net.commoble.morered.mechanisms;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.MoreRed;
import net.commoble.morered.mechanisms.WindcatcherRecipe.XY;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags.Items;

public class WindcatcherDyeRecipe extends CustomRecipe
{

	public static final MapCodec<WindcatcherDyeRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(WindcatcherDyeRecipe::category),
			Ingredient.CODEC.fieldOf("ingredient").forGetter(WindcatcherDyeRecipe::ingredient),
			XY.CODEC.optionalFieldOf("north", XY.NORTH).forGetter(WindcatcherDyeRecipe::north),
			XY.CODEC.optionalFieldOf("south", XY.SOUTH).forGetter(WindcatcherDyeRecipe::south),
			XY.CODEC.optionalFieldOf("west", XY.WEST).forGetter(WindcatcherDyeRecipe::west),
			XY.CODEC.optionalFieldOf("east", XY.EAST).forGetter(WindcatcherDyeRecipe::east),
			XY.CODEC.optionalFieldOf("windcatcher", XY.CENTER).forGetter(WindcatcherDyeRecipe::windcatcher)
		).apply(builder, WindcatcherDyeRecipe::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, WindcatcherDyeRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
	
	private final Ingredient ingredient; public Ingredient ingredient() { return this.ingredient; }
	private final XY north; public XY north() { return this.north; }
	private final XY south; public XY south() { return this.south; }
	private final XY west; public XY west() { return this.west; }
	private final XY east; public XY east() { return this.east; }
	private final XY windcatcher; public XY windcatcher() { return this.windcatcher; }
	
	public WindcatcherDyeRecipe(CraftingBookCategory category, Ingredient ingredient,
		XY north, XY south, XY west, XY east, XY windcatcher
		)
	{
		super(category);
		this.ingredient = ingredient;
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
		this.windcatcher = windcatcher;
	}
	
	public static WindcatcherDyeRecipe of(Ingredient ingredient)
	{
		return new WindcatcherDyeRecipe(
			CraftingBookCategory.MISC,
			ingredient,
			XY.NORTH,
			XY.SOUTH,
			XY.WEST,
			XY.EAST,
			XY.CENTER);
	}

	@Override
	public boolean matches(CraftingInput input, Level level)
	{
		XY offset = findOffset(input);
		Predicate<ItemStack> dyeTest = stack -> stack.is(Items.DYES);
		// dye ingredients are optional but at least one is required to be in the input
		return this.hasItem(input, windcatcher.add(offset), ingredient) && (
				this.hasItem(input,north.add(offset), dyeTest)
				|| this.hasItem(input, south.add(offset), dyeTest)
				|| this.hasItem(input, west.add(offset), dyeTest)
				|| this.hasItem(input, east.add(offset), dyeTest)
			);
	}

	@Override
	public ItemStack assemble(CraftingInput input, Provider provider)
	{
		XY offset = findOffset(input);
		XY actualWindcatcherCoord = this.windcatcher.add(offset);
		ItemStack windcatcher = input.getItem(actualWindcatcherCoord.x(), actualWindcatcherCoord.y());
		WindcatcherColors colors = Objects.requireNonNullElse(windcatcher.get(MoreRed.WINDCATCHER_COLORS_DATA_COMPONENT.get()), WindcatcherColors.DEFAULT);
		DyeColor north = this.getColor(input, this.north.add(offset), colors.north());
		DyeColor south = this.getColor(input, this.south.add(offset), colors.south());
		DyeColor west = this.getColor(input, this.west.add(offset), colors.west());
		DyeColor east = this.getColor(input, this.east.add(offset), colors.east());
		WindcatcherColors newColors = new WindcatcherColors(north,south,west,east);
		ItemStack result = windcatcher.copyWithCount(1);
		result.set(MoreRed.WINDCATCHER_COLORS_DATA_COMPONENT.get(), newColors);
		return result;
	}
	
	public boolean hasItem(CraftingInput input, XY xy, Predicate<ItemStack> predicate)
	{
		int x = xy.x();
		int y = xy.y();
		if (x < 0 || y < 0 || input.width() <= x || input.height() <= y)
		{
			return false;
		}
		ItemStack stack = input.getItem(x,y);
		return predicate.test(stack);
	}
	
	public Optional<DyeColor> getColor(ItemStack input)
	{
		for (DyeColor color : DyeColor.values())
		{
			if (input.is(color.getTag()))
			{
				return Optional.of(color);
			}
		}
		return Optional.empty();
	}
	
	public DyeColor getColor(CraftingInput input, XY xy, DyeColor defaultColor)
	{
		int x = xy.x();
		int y = xy.y();
		if (x < 0 || y < 0 || input.width() <= x || input.height() <= y)
		{
			return defaultColor;
		}
		return getColor(input.getItem(x,y)).orElse(defaultColor);
	}
	
	public XY findOffset(CraftingInput input)
	{
		int width = input.width();
		int height = input.height();
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (this.ingredient.test(input.getItem(x,y)))
				{
					return new XY(x,y).subtract(this.windcatcher);
				}
			}
		}
		return XY.ZERO;
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer()
	{
		return MoreRed.WINDCATCHER_DYE_RECIPE_SERIALIZER.get();
	}
}
