package net.commoble.morered.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public record LogicGateTintSource(Block block, SequencedSet<Integer> forceTintindexesLit) implements ItemTintSource
{
	public static final MapCodec<LogicGateTintSource> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(LogicGateTintSource::block),
			Codec.INT.listOf().<SequencedSet<Integer>>xmap(LinkedHashSet::new, List::copyOf).optionalFieldOf("force_tintindexes_lit", new LinkedHashSet<>()).forGetter(LogicGateTintSource::forceTintindexesLit)
		).apply(builder, LogicGateTintSource::new));
		
		
	@Override
	public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity)
	{
		
		return 0;
	}

	@Override
	public MapCodec<? extends ItemTintSource> type()
	{
		return CODEC;
	}

}
