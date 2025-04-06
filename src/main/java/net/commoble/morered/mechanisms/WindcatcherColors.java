package net.commoble.morered.mechanisms;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

public record WindcatcherColors(
	DyeColor north,
	DyeColor south,
	DyeColor west,
	DyeColor east)
{
	public static final WindcatcherColors DEFAULT = new WindcatcherColors(DyeColor.WHITE, DyeColor.WHITE, DyeColor.WHITE, DyeColor.WHITE);
	
	public static final Codec<WindcatcherColors> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			DyeColor.CODEC.fieldOf("north").forGetter(WindcatcherColors::north),
			DyeColor.CODEC.fieldOf("south").forGetter(WindcatcherColors::south),
			DyeColor.CODEC.fieldOf("west").forGetter(WindcatcherColors::west),
			DyeColor.CODEC.fieldOf("east").forGetter(WindcatcherColors::east)
		).apply(builder, WindcatcherColors::new));
	
	public static final StreamCodec<ByteBuf, WindcatcherColors> STREAM_CODEC = StreamCodec.composite(
		DyeColor.STREAM_CODEC, WindcatcherColors::north,
		DyeColor.STREAM_CODEC, WindcatcherColors::south,
		DyeColor.STREAM_CODEC, WindcatcherColors::west,
		DyeColor.STREAM_CODEC, WindcatcherColors::east,
		WindcatcherColors::new);
	
	public Map<Direction,DyeColor> toMap()
	{
		return Map.of(
			Direction.NORTH, north,
			Direction.SOUTH, south,
			Direction.WEST, west,
			Direction.EAST, east);
	}
	
	public CompoundTag toTag()
	{
		CompoundTag tag = new CompoundTag();
		tag.putString("north", north.getName());
		tag.putString("south", south.getName());
		tag.putString("west", west.getName());
		tag.putString("east", east.getName());
		return tag;
	}
	
	public static WindcatcherColors fromTag(CompoundTag tag)
	{
		return new WindcatcherColors(
			DyeColor.byName(tag.getStringOr("north", ""), DyeColor.WHITE),
			DyeColor.byName(tag.getStringOr("south", ""), DyeColor.WHITE),
			DyeColor.byName(tag.getStringOr("west", ""), DyeColor.WHITE),
			DyeColor.byName(tag.getStringOr("east", ""), DyeColor.WHITE)
		);
	}
}
