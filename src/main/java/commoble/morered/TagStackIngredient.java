package commoble.morered;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class TagStackIngredient {
    public static final IIngredientSerializer<Ingredient> SERIALIZER = new IIngredientSerializer<Ingredient>() {

        @Override
        public void write(FriendlyByteBuf buffer, Ingredient ingredient) {
            ItemStack[] items = ingredient.getItems();
            buffer.writeVarInt(items.length);    // tell the packet how long the array is
            for (ItemStack stack : items) {
                buffer.writeItem(stack);
            }
        }

        @Override
        public Ingredient parse(FriendlyByteBuf buffer) {
//			return Ingredient.fromValues(Stream.generate(() -> new Ingredient.SingleItemList(buffer.readItem())).limit
//			(buffer.readVarInt()));
            return Ingredient.of(buffer.readItem());
        }

        @Override
        public Ingredient parse(JsonObject json) {
            ResourceLocation tagID = new ResourceLocation(GsonHelper.getAsString(json, "tag")); // throws
            // JsonSyntaxException if no tag field
//			int count = GsonHelper.getAsInt(json, "count", 1);
            Tag<Item> tag = ItemTags.getAllTags().getTag(tagID); // can return null if tag is invalid
            if (tag == null) {
                throw new JsonSyntaxException("Unknown item tag '" + tagID + "'"); // will get caught and logged
                // during data loading
            }
//			return Ingredient.fromValues(tag.getValues().stream().map(item -> new Ingredient.SingleItemList(new ItemStack(item, count))));
            return Ingredient.of(tag);
        }

    };

}
