package commoble.morered;

import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.ITag;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class TagStackIngredient
{
	public static final IIngredientSerializer<Ingredient> SERIALIZER = new IIngredientSerializer<Ingredient>()
	{

		@Override
		public void write(PacketBuffer buffer, Ingredient ingredient)
		{
			ItemStack[] items = ingredient.getMatchingStacks();
			buffer.writeVarInt(items.length);	// tell the packet how long the array is
			for (ItemStack stack : items)
			{
				buffer.writeItemStack(stack);
			}
		}

		@Override
		public Ingredient parse(PacketBuffer buffer)
		{
			return Ingredient.fromItemListStream(Stream.generate(() -> new Ingredient.SingleItemList(buffer.readItemStack())).limit(buffer.readVarInt()));
		}

		@Override
		public Ingredient parse(JsonObject json)
		{
			ResourceLocation tagID = new ResourceLocation(JSONUtils.getString(json, "tag")); // throws JsonSyntaxException if no tag field
			int count = JSONUtils.getInt(json, "count", 1);
			ITag<Item> tag = TagCollectionManager.getManager().getItemTags().get(tagID); // can return null if tag is invalid
			if (tag == null)
			{
				throw new JsonSyntaxException("Unknown item tag '" + tagID + "'"); // will get caught and logged during data loading
			}
			return Ingredient.fromItemListStream(tag.getAllElements().stream().map(item -> new Ingredient.SingleItemList(new ItemStack(item, count))));
		}

	};

}
