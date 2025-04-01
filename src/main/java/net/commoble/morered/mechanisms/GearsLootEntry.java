package net.commoble.morered.mechanisms;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class GearsLootEntry extends LootPoolSingletonContainer
{
	public static final MapCodec<GearsLootEntry> CODEC = RecordCodecBuilder.mapCodec(builder -> singletonFields(builder)
	    .apply(builder, GearsLootEntry::new));

	protected GearsLootEntry(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions)
	{
		super(weight, quality, conditions, functions);
	}

	@Override
	protected void createItemStack(Consumer<ItemStack> stackDropper, LootContext context)
	{
		BlockState state = context.getParameter(LootContextParams.BLOCK_STATE);
		if (state.getBlock() instanceof GearsBlock
			&& context.getParameter(LootContextParams.BLOCK_ENTITY) instanceof GenericBlockEntity be)
		{
			var items = be.get(MoreRed.get().gearsDataComponent.get());
			for (Direction dir : Direction.values())
			{
				if (state.getValue(FaceSegmentBlock.getProperty(dir)))
				{
					ItemStack stack = items.get(dir);
					if (stack != null)
					{
						stackDropper.accept(stack);
					}
				}
			}
		}
	}

	@Override
	public LootPoolEntryType getType()
	{
		return MoreRed.get().gearsLootEntry.get();
	}

    public static LootPoolSingletonContainer.Builder<?> gearsLoot() {
        return simpleBuilder(GearsLootEntry::new);
    }
}
