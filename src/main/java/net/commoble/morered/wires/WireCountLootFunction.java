package net.commoble.morered.wires;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

// loot function to set the count of a wire block's loot's dropped wire items
// multiple wire blockitems can be placed into a block,
// so we want the block to drop the correct number of wires based on its state
public class WireCountLootFunction implements LootItemFunction
{
	public static final WireCountLootFunction INSTANCE = new WireCountLootFunction();
	
	public static final MapCodec<WireCountLootFunction> CODEC = MapCodec.unit(INSTANCE);

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
	public LootItemFunctionType<WireCountLootFunction> getType()
	{
		return MoreRed.get().wireCountLootFunction.get();
	}
	
	@Override
	public Set<LootContextParam<?>> getReferencedContextParams()
	{
		return ImmutableSet.of(LootContextParams.BLOCK_STATE);
	}
}
