package commoble.morered.wires;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.functions.ILootFunction;

// loot function to set the count of a wire block's loot's dropped wire items
// multiple wire blockitems can be placed into a block,
// so we want the block to drop the correct number of wires based on its state
public class WireCountLootFunction implements ILootFunction, ILootSerializer<WireCountLootFunction>
{
	public static final WireCountLootFunction INSTANCE = new WireCountLootFunction();
	public static final LootFunctionType TYPE = new LootFunctionType(INSTANCE);

	@Override
	public ItemStack apply(ItemStack input, LootContext context)
	{
		BlockState state = context.getParamOrNull(LootParameters.BLOCK_STATE);
		Block block = state.getBlock();
		if (block instanceof AbstractWireBlock)
		{
			input.setCount(((AbstractWireBlock)block).getWireCount(state));
		}
		return input;
	}

	@Override
	public LootFunctionType getType()
	{
		return TYPE;
	}
	
	@Override
	public Set<LootParameter<?>> getReferencedContextParams()
	{
		return ImmutableSet.of(LootParameters.BLOCK_STATE);
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
