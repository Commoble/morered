package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.joml.Vector3f;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.MoreRed;
import net.commoble.morered.mechanisms.WindcatcherColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModel.BakingContext;
import net.minecraft.client.renderer.item.ItemModel.Unbaked;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.SimpleModelState;

public record UnbakedWindcatcherModel(ResourceLocation axle, ResourceLocation airfoil, Map<DyeColor,ResourceLocation> airfoilSails) implements ItemModel.Unbaked
{
	public static final MapCodec<UnbakedWindcatcherModel> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			ResourceLocation.CODEC.fieldOf("axle").forGetter(UnbakedWindcatcherModel::axle),
			ResourceLocation.CODEC.fieldOf("airfoil").forGetter(UnbakedWindcatcherModel::airfoil),
			Codec.simpleMap(DyeColor.CODEC, ResourceLocation.CODEC, StringRepresentable.keys(DyeColor.values())).codec().fieldOf("airfoil_sails").forGetter(UnbakedWindcatcherModel::airfoilSails)
		).apply(builder, UnbakedWindcatcherModel::new));

	@Override
	public void resolveDependencies(Resolver resolver)
	{
		resolver.resolve(this.axle);
		resolver.resolve(this.airfoil);
		this.airfoilSails.values().forEach(resolver::resolve);
	}

	@Override
	public MapCodec<? extends Unbaked> type()
	{
		return CODEC;
	}

	@Override
	public ItemModel bake(BakingContext context)
	{
		var baker = context.blockModelBaker();
		Map<SailKey,ItemModel> bakedSails = new HashMap<>();
		List<ItemModel> bakedFoils = new ArrayList<>();
		for (int i=0; i<4; i++)
		{
			Direction dir = Direction.from2DDataValue(i);
			int x = dir.getStepX();
			int z = dir.getStepZ();
			ModelState modelState = new SimpleModelState(new Transformation(new Vector3f(x,0,z),null,null,null));
			bakedFoils.add(new BlockModelWrapper(baker.bake(airfoil, modelState), List.of()));
			airfoilSails.forEach((color,id) ->
				bakedSails.put(new SailKey(color,dir), new BlockModelWrapper(baker.bake(id, modelState), List.of())));
		}
		return new WindcatcherModel(
			new BlockModelWrapper(context.bake(this.axle), List.of()),
			bakedFoils,
			bakedSails);
	}

	public static record WindcatcherModel(ItemModel axle, List<ItemModel> airfoils, Map<SailKey,ItemModel> airfoilSails) implements ItemModel
	{
		@Override
		public void update(
			ItemStackRenderState renderState,
			ItemStack stack,
			ItemModelResolver resolver,
			ItemDisplayContext displayContext,
			ClientLevel level,
			LivingEntity entity,
			int seed)
		{
			renderState.ensureCapacity(9); // 1 + 4 + 4
			axle.update(renderState, stack, resolver, displayContext, level, entity, seed);
			for (ItemModel model : airfoils)
			{
				model.update(renderState, stack, resolver, displayContext, level, entity, seed);
			}
			WindcatcherColors colors = Objects.requireNonNullElse(stack.get(MoreRed.get().windcatcherColorsDataComponent.get()), WindcatcherColors.DEFAULT);
			airfoilSails.get(new SailKey(colors.north(), Direction.NORTH)).update(renderState, stack, resolver, displayContext, level, entity, seed);
			airfoilSails.get(new SailKey(colors.south(), Direction.SOUTH)).update(renderState, stack, resolver, displayContext, level, entity, seed);
			airfoilSails.get(new SailKey(colors.west(), Direction.WEST)).update(renderState, stack, resolver, displayContext, level, entity, seed);
			airfoilSails.get(new SailKey(colors.east(), Direction.EAST)).update(renderState, stack, resolver, displayContext, level, entity, seed);
		}
		
	}
	
	private static record SailKey(DyeColor color, Direction dir) {}
}
