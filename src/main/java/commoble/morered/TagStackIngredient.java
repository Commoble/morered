package commoble.morered;

import java.util.Collection;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Ingredient.TagValue;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class TagStackIngredient
{
	public static final IIngredientSerializer<Ingredient> SERIALIZER = new IIngredientSerializer<Ingredient>()
	{
		@Override
		public void write(FriendlyByteBuf buffer, Ingredient ingredient)
		{
			ItemStack[] items = ingredient.getItems();
			buffer.writeVarInt(items.length);	// tell the packet how long the array is
			for (ItemStack stack : items)
			{
				buffer.writeItem(stack);
			}
		}

		@Override
		public Ingredient parse(FriendlyByteBuf buffer)
		{
			return Ingredient.fromValues(Stream.generate(() -> new Ingredient.ItemValue(buffer.readItem())).limit(buffer.readVarInt()));
		}

		@Override
		public Ingredient parse(JsonObject json)
		{
			ResourceLocation tagID = new ResourceLocation(GsonHelper.getAsString(json, "tag")); // throws JsonSyntaxException if no tag field
			int count = GsonHelper.getAsInt(json, "count", 1);
			TagCountValue value = new TagCountValue(TagKey.create(Registry.ITEM_REGISTRY, tagID), count);
			return Ingredient.fromValues(Stream.of(value));
		}

	};
	
	public static class TagCountValue extends TagValue
	{
		private final int count;
		public int count() { return this.count; }

		public TagCountValue(TagKey<Item> tag, int count)
		{
			super(tag);
			this.count = count;
		}

		@Override
		public Collection<ItemStack> getItems()
		{
			var items = super.getItems();
			items.forEach(stack -> stack.setCount(this.count));
			return items;
		}

		@Override
		public JsonObject serialize()
		{
			JsonObject obj = super.serialize();
			obj.addProperty("count", this.count());
			return obj;
		}
	}
}
