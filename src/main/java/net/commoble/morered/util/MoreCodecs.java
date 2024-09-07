package net.commoble.morered.util;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

public class MoreCodecs
{
	public static final StreamCodec<ByteBuf,Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.DOUBLE, Vec3::x,
		ByteBufCodecs.DOUBLE, Vec3::y,
		ByteBufCodecs.DOUBLE, Vec3::z,
		Vec3::new);
	
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> dispatch(ResourceKey<Registry<MapCodec<? extends T>>> registryKey, Function<? super T, ? extends MapCodec<? extends T>> typeCodec)
	{
		return Codec.lazyInitialized(() -> 
			((Registry<MapCodec<? extends T>>)BuiltInRegistries.REGISTRY.get(registryKey))
			.byNameCodec()
			.dispatch(typeCodec, mapCodec -> mapCodec));
	}
}
