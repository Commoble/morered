package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.plate_blocks.LogicFunctions;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModel.BakingContext;
import net.minecraft.client.renderer.item.ItemModel.Unbaked;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.resources.Identifier;

public record UnbakedLogicGateModel(Identifier model) implements ItemModel.Unbaked
{
	public static final MapCodec<UnbakedLogicGateModel> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Identifier.CODEC.fieldOf("model").forGetter(UnbakedLogicGateModel::model)
		).apply(builder, UnbakedLogicGateModel::new));
	
	public static final Supplier<List<ItemTintSource>> TINTS = Suppliers.memoize(() -> {
		List<ItemTintSource> tints = new ArrayList<>();
		int maxIndex = LogicFunctions.maxIndex();
		for (int i=0; i<= maxIndex; i++)
		{
			tints.add(new Constant(ColorHandlers.getLogicFunctionTint(i, false, false, false)));
		}
		tints.set(LogicFunctions.SET_LATCH, new Constant(ColorHandlers.UNLIT));
		tints.set(LogicFunctions.UNSET_LATCH, new Constant(ColorHandlers.LIT));
		return tints;
	});

	@Override
	public void resolveDependencies(Resolver resolver)
	{
		resolver.markDependency(model);
	}

	@Override
	public MapCodec<? extends Unbaked> type()
	{
		return CODEC;
	}

	@Override
	public ItemModel bake(BakingContext context)
	{
		return ModelUtil.wrapBlockModel(context, model, BlockModelRotation.IDENTITY, TINTS.get());
	}

}
