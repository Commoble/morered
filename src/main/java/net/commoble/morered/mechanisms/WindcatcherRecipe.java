package net.commoble.morered.mechanisms;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.MoreRed;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class WindcatcherRecipe extends ShapedRecipe
{
	public static final MapCodec<WindcatcherRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Codec.STRING.optionalFieldOf("group", "").forGetter(WindcatcherRecipe::group),
			CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(WindcatcherRecipe::category),
			ShapedRecipePattern.MAP_CODEC.forGetter(WindcatcherRecipe::pattern),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WindcatcherRecipe::result),
			Codec.BOOL.optionalFieldOf("show_notification", Boolean.valueOf(true)).forGetter(WindcatcherRecipe::showNotification),
			DyeColor.CODEC.optionalFieldOf("default_color", DyeColor.WHITE).forGetter(WindcatcherRecipe::defaultColor),
			XY.CODEC.optionalFieldOf("north", XY.NORTH).forGetter(WindcatcherRecipe::north),
			XY.CODEC.optionalFieldOf("south", XY.SOUTH).forGetter(WindcatcherRecipe::south),
			XY.CODEC.optionalFieldOf("west", XY.WEST).forGetter(WindcatcherRecipe::west),
			XY.CODEC.optionalFieldOf("east", XY.EAST).forGetter(WindcatcherRecipe::east)
		).apply(builder, WindcatcherRecipe::new));
	
	// too many fields for StreamCodec.composite, just wrap the nbt codec
	public static final StreamCodec<RegistryFriendlyByteBuf, WindcatcherRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
	
	public ShapedRecipePattern pattern() { return this.pattern; }
	private final ItemStack result; public ItemStack result() { return this.result; }
	private final DyeColor defaultColor; public DyeColor defaultColor() { return this.defaultColor; }
	private final XY north; public XY north() { return this.north; }
	private final XY south; public XY south() { return this.south; }
	private final XY west; public XY west() { return this.west; }
	private final XY east; public XY east() { return this.east; }
	
	public WindcatcherRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack stack, boolean showNotification,
		DyeColor defaultColor, XY north, XY south, XY west, XY east
		)
	{
		super(group, category, pattern, stack, showNotification);
		this.result = stack;
		this.defaultColor = defaultColor;
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
	}
	
	public static WindcatcherRecipe of(Item item, List<String> pattern, Map<Character,Ingredient> key)
	{
		return new WindcatcherRecipe("", CraftingBookCategory.MISC, ShapedRecipePattern.of(key, pattern), new ItemStack(item,1), true,
			DyeColor.WHITE,
			XY.NORTH,
			XY.SOUTH,
			XY.WEST,
			XY.EAST);
	}

	@Override
	public boolean isSpecial()
	{
		return true;
	}

	@Override
	public RecipeSerializer<? extends ShapedRecipe> getSerializer()
	{
		return MoreRed.get().windcatcherRecipeSerializer.get();
	}
	
	public DyeColor getColor(ItemStack input)
	{
		for (DyeColor color : DyeColor.values())
		{
			if (input.is(color.getDyedTag()))
			{
				return color;
			}
		}
		return this.defaultColor;
	}
	
	public DyeColor getColor(CraftingInput input, XY xy)
	{
		int x = xy.x;
		int y = xy.y;
		if (this.getWidth() <= x || this.getHeight() <= y)
		{
			return this.defaultColor;
		}
		return getColor(input.getItem(x,y));
	}

	@Override
	public ItemStack assemble(CraftingInput input, Provider provider)
	{
		ItemStack output = super.assemble(input, provider);
		
		WindcatcherColors colors = new WindcatcherColors(
			this.getColor(input, this.north),
			this.getColor(input, this.south),
			this.getColor(input, this.west),
			this.getColor(input, this.east));
		
		output.set(MoreRed.get().windcatcherColorsDataComponent.get(), colors);
		return output;
	}
	
	public static record XY(int x, int y) {
		public static final Codec<XY> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("x").forGetter(XY::x),
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("y").forGetter(XY::y)
			).apply(builder, XY::new));
		
		public static final XY ZERO = new XY(0,0);
		public static final XY NORTH = new XY(1,0);
		public static final XY SOUTH = new XY(1,2);
		public static final XY WEST = new XY(0,1);
		public static final XY EAST = new XY(2,1);
		public static final XY CENTER = new XY(1,1);
		
		public XY add(XY that)
		{
			return new XY(this.x + that.x, this.y + that.y);
		}
		
		public XY subtract(XY that)
		{
			return new XY(this.x - that.x, this.y - that.y);
		}
	}
}
