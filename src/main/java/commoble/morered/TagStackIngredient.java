package commoble.morered;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.ITag;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class TagStackIngredient extends Ingredient
{
	private final ITag<Item> tag;	public ITag<Item> getTag() { return this.tag;}
	private final int count;	public final int getCount() { return this.count; }
	
	public TagStackIngredient(ITag<Item> tag, int count)
	{
		super(makeItemStacks(tag, count));
		this.tag = tag;
		this.count = count;
	}

    @Override
    public boolean isSimple()
    {
        return false;
    }
    
    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer()
    {
        return SERIALIZER;
    }
    
    public static Stream<TagStackList> makeItemStacks(ITag<Item> tag, int count)
    {
    	return tag == null
    		? Stream.of() // invalid tag, no stacks to match to
    		: Stream.of(new TagStackList(tag, count));
    }
    
    public static final IIngredientSerializer<TagStackIngredient> SERIALIZER = new IIngredientSerializer<TagStackIngredient>()
	{

		@Override
		public void write(PacketBuffer buffer, TagStackIngredient ingredient)
		{
			buffer.writeString(TagCollectionManager.getManager().getItemTags().getValidatedIdFromTag(ingredient.tag).toString());
			buffer.writeInt(ingredient.count);
		}

		@Override
		public TagStackIngredient parse(PacketBuffer buffer)
		{
			String tagName = buffer.readString();
			int count = buffer.readInt();
			ITag<Item> tag = TagCollectionManager.getManager().getItemTags().get(new ResourceLocation(tagName)); // can return null if tag is invalid
			return new TagStackIngredient(tag, count);
		}

		@Override
		public TagStackIngredient parse(JsonObject json)
		{
			ResourceLocation tagID = new ResourceLocation(JSONUtils.getString(json, "tag")); // throws JsonSyntaxException if no tag field
			int count = JSONUtils.getInt(json, "count", 1);
			ITag<Item> tag = TagCollectionManager.getManager().getItemTags().get(tagID); // can return null if tag is invalid
			return new TagStackIngredient(tag, count);
		}
	
	};
	
	public static class TagStackList implements Ingredient.IItemList
	{
		private final @Nonnull ITag<Item> tag;
		private final int count;
		private final Collection<ItemStack> stacks;
		
		public TagStackList(@Nonnull ITag<Item> tag, int count)
		{
			this.tag = tag;
			this.count = count;
			
			this.stacks = tag.getAllElements()
				.stream()
				.map(item -> new ItemStack(item, count))
				.collect(Collectors.toList());
		}

		@Override
		public Collection<ItemStack> getStacks()
		{
			return this.stacks;
		}

		@Override
		public JsonObject serialize()
		{
			JsonObject json = new JsonObject();
			
			json.addProperty("tag", TagCollectionManager.getManager().getItemTags().getValidatedIdFromTag(this.tag).toString());
			
			if (this.count > 1)
			{
				json.addProperty("count", this.count);
			}
			
			return json;
		}
		
	}
}
