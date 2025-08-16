package net.commoble.morered.mechanisms;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;

public sealed interface Wind permits Wind.ConstantWind, Wind.LinearWind
{
	public static final Codec<Wind> CODEC = Codec.either(ConstantWind.CODEC, LinearWind.CODEC)
		.xmap(
			either -> either.map(Function.identity(), Function.identity()),
			wind -> switch(wind) {
				case ConstantWind c -> Either.left(c);
				case LinearWind l -> Either.right(l);
			});
	
	/**
	 * Get how windy it is somewhere
	 * @param level LevelReader where the wind is
	 * @param pos BlockPos where a windmill is
	 * @return int wind level in the range [0,8]
	 */
	public static int getWind(LevelReader level, BlockPos pos) {
		var dimensionTypes = level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
		return dimensionTypes.getResourceKey(level.dimensionType()).map(key -> {
			@Nullable Wind wind = dimensionTypes.getData(MoreRed.WIND_DATA_MAP_TYPE, key);
			return wind == null ? 0 : wind.getWindLevel(level, pos); 
		}).orElse(0);
	}
		
	/**
	 * Get how windy it is somewhere
	 * @param level LevelReader where the wind is
	 * @param pos BlockPos where a windmill is
	 * @return int wind level in the range [0,8]
	 */
	public int getWindLevel(LevelReader level, BlockPos pos);
	
	public static record ConstantWind(int value) implements Wind
	{
		public static final Codec<ConstantWind> CODEC = Codec.intRange(0, 8)
			.xmap(ConstantWind::new, ConstantWind::value)
			.fieldOf("value")
			.codec();

		@Override
		public int getWindLevel(LevelReader level, BlockPos pos)
		{
			return this.value;
		}
	}
	
	public static record LinearWind(int minY, int maxY) implements Wind
	{
		public static final Codec<LinearWind> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.INT.fieldOf("min_y").forGetter(LinearWind::minY),
				Codec.INT.fieldOf("max_y").forGetter(LinearWind::maxY)
			).apply(builder, LinearWind::new));

		@Override
		public int getWindLevel(LevelReader level, BlockPos pos)
		{
			int y = pos.getY();
			int minY = level.getMinY();
			int maxY = level.getMaxY();
			if (y < minY)
			{
				return 0;
			}
			if (y >= maxY)
			{
				return 8;
			}
			int yIncrement = (this.maxY - this.minY) / 7; // we start at 1 and wind=8 doesn't count, there are seven wind layers below max
			int heightAboveMin = y - minY;
			int increments = heightAboveMin / yIncrement;
			return increments + 1;
		}
	}
}
