package net.commoble.morered.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

public class MoreCodecs
{
	public static final Codec<Set<BlockPos>> POSITIONS_CODEC = BlockPos.CODEC.listOf().xmap(HashSet::new, ArrayList::new);
	public static final MapCodec<Set<BlockPos>> POSITIONS_MAP_CODEC = POSITIONS_CODEC.fieldOf("positions");
	public static final StreamCodec<ByteBuf,Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.DOUBLE, Vec3::x,
		ByteBufCodecs.DOUBLE, Vec3::y,
		ByteBufCodecs.DOUBLE, Vec3::z,
		Vec3::new);
	
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> dispatch(ResourceKey<Registry<MapCodec<? extends T>>> registryKey, Function<? super T, ? extends MapCodec<? extends T>> typeCodec)
	{
		return Codec.lazyInitialized(() -> {
			// eclipsec and javac need to agree on the generics, so this might look strange
			Registry<?> uncastRegistry = BuiltInRegistries.REGISTRY.getValue(registryKey.location());
			Registry<MapCodec<? extends T>> registry = (Registry<MapCodec<? extends T>>) uncastRegistry;
			return registry.byNameCodec().dispatch(typeCodec, Function.identity());
		});
	}
}
