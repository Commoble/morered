package net.commoble.morered.mechanisms;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.MoreRed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class WindcatcherRecipe extends NormalCraftingRecipe
{
	public static final MapCodec<WindcatcherRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			CommonInfo.MAP_CODEC.forGetter(WindcatcherRecipe::commonInfo),
			CraftingBookInfo.MAP_CODEC.forGetter(WindcatcherRecipe::craftingBookInfo),
			ShapedRecipePattern.MAP_CODEC.forGetter(WindcatcherRecipe::pattern),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(WindcatcherRecipe::result),
			DyeColor.CODEC.optionalFieldOf("default_color", DyeColor.WHITE).forGetter(WindcatcherRecipe::defaultColor),
			XY.CODEC.optionalFieldOf("north", XY.NORTH).forGetter(WindcatcherRecipe::north),
			XY.CODEC.optionalFieldOf("south", XY.SOUTH).forGetter(WindcatcherRecipe::south),
			XY.CODEC.optionalFieldOf("west", XY.WEST).forGetter(WindcatcherRecipe::west),
			XY.CODEC.optionalFieldOf("east", XY.EAST).forGetter(WindcatcherRecipe::east)
		).apply(builder, WindcatcherRecipe::new));
	
	// too many fields for StreamCodec.composite, just wrap the nbt codec
	public static final StreamCodec<RegistryFriendlyByteBuf, WindcatcherRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
	
	public CommonInfo commonInfo() { return this.commonInfo; }
	public CraftingBookInfo craftingBookInfo() { return this.bookInfo; }
	private final ShapedRecipePattern pattern; public ShapedRecipePattern pattern() { return this.pattern; }
	private final ItemStackTemplate result; public ItemStackTemplate result() { return this.result; }
	private final DyeColor defaultColor; public DyeColor defaultColor() { return this.defaultColor; }
	private final XY north; public XY north() { return this.north; }
	private final XY south; public XY south() { return this.south; }
	private final XY west; public XY west() { return this.west; }
	private final XY east; public XY east() { return this.east; }
	
	public WindcatcherRecipe(
		CommonInfo commonInfo,
		CraftingBookInfo craftingBookInfo,
		ShapedRecipePattern pattern,
		ItemStackTemplate stack,
		DyeColor defaultColor, XY north, XY south, XY west, XY east
		)
	{
		super(commonInfo, craftingBookInfo);
		this.pattern = pattern;
		this.result = stack;
		this.defaultColor = defaultColor;
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
	}
	
	public static WindcatcherRecipe of(Item item, List<String> pattern, Map<Character,Ingredient> key)
	{
		return new WindcatcherRecipe(
			new CommonInfo(true),
			new CraftingBookInfo(CraftingBookCategory.MISC, ""),
			ShapedRecipePattern.of(key, pattern), new ItemStackTemplate(item,1),
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
	public RecipeSerializer<? extends NormalCraftingRecipe> getSerializer()
	{
		return MoreRed.WINDCATCHER_RECIPE_SERIALIZER.get();
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
		if (this.pattern.width() <= x || this.pattern.height() <= y)
		{
			return this.defaultColor;
		}
		return getColor(input.getItem(x,y));
	}

	@Override
	public boolean matches(CraftingInput input, Level level)
	{
		return this.pattern.matches(input);
	}
	
	@Override
	public ItemStack assemble(CraftingInput input)
	{
		ItemStack output = this.result.create();
		
		WindcatcherColors colors = new WindcatcherColors(
			this.getColor(input, this.north),
			this.getColor(input, this.south),
			this.getColor(input, this.west),
			this.getColor(input, this.east));
		
		output.set(MoreRed.WINDCATCHER_COLORS_DATA_COMPONENT.get(), colors);
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

	@Override
	protected PlacementInfo createPlacementInfo()
	{
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
	}

	@Override
	public List<RecipeDisplay> display()
	{
		return List.of(
			new ShapedCraftingRecipeDisplay(
				this.pattern.width(),
				this.pattern.height(),
				this.pattern.ingredients().stream().map(e -> e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
				new SlotDisplay.ItemStackSlotDisplay(this.result),
				new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
	}
}
