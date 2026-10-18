package net.commoble.morered.mechanisms;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class GearsLootEntry extends SingleEntryContainerBase
{
	public static final MapCodec<GearsLootEntry> CODEC = RecordCodecBuilder.mapCodec(builder -> uniformFields(builder)
	    .apply(builder, GearsLootEntry::new));

	protected GearsLootEntry(int weight, int quality, Optional<Holder<LootItemCondition>> conditions, Optional<Holder<LootItemFunction>> functions)
	{
		super(weight, quality, conditions, functions);
	}

	@Override
	protected void createItemStack(Consumer<ItemStack> stackDropper, LootContext context)
	{
		@Nullable BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
		if (state != null && state.getBlock() instanceof GearsBlock
			&& context.getOptional(LootContextParams.BLOCK_ENTITY) instanceof GenericBlockEntity be)
		{
			var items = be.get(MoreRed.GEARS_DATA_COMPONENT.get());
			if (items == null)
				return;
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
	public MapCodec<? extends SingleEntryContainerBase> codec()
	{
		return MoreRed.GEARS_LOOT_ENTRY.get();
	}

    public static SingleEntryContainerBase.Builder<?> gearsLoot() {
        return simpleBuilder(GearsLootEntry::new);
    }
}
