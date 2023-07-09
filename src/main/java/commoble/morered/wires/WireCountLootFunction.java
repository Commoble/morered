package commoble.morered.wires;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import commoble.morered.MoreRed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

// loot function to set the count of a wire block's loot's dropped wire items
// multiple wire blockitems can be placed into a block,
// so we want the block to drop the correct number of wires based on its state
public class WireCountLootFunction implements LootItemFunction, Serializer<WireCountLootFunction>
{
	public static final WireCountLootFunction INSTANCE = new WireCountLootFunction();

	@Override
	public ItemStack apply(ItemStack input, LootContext context)
	{
		BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
		Block block = state.getBlock();
		if (block instanceof AbstractWireBlock)
		{
			input.setCount(((AbstractWireBlock)block).getWireCount(state));
		}
		return input;
	}

	@Override
	public LootItemFunctionType getType()
	{
		return MoreRed.get().wireCountLootFunction.get();
	}
	
	@Override
	public Set<LootContextParam<?>> getReferencedContextParams()
	{
		return ImmutableSet.of(LootContextParams.BLOCK_STATE);
	}

	// we can put the serializer on the same class as there's no data here
	@Override
	public void serialize(JsonObject json, WireCountLootFunction function, JsonSerializationContext context)
	{
		// noop, no data
	}

	@Override
	public WireCountLootFunction deserialize(JsonObject json, JsonDeserializationContext context)
	{
		// noop, no data
		return INSTANCE;
	}
}
