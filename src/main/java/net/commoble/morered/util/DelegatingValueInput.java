package net.commoble.morered.util;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.level.storage.ValueInput;

public interface DelegatingValueInput extends ValueInput
{
	public abstract ValueInput delegate();

	@Override
	default <T> Optional<T> read(String field, Codec<T> codec)
	{
		return delegate().read(field, codec);
	}

	@Override
	@Deprecated
	default <T> Optional<T> read(MapCodec<T> codec)
	{
		return delegate().read(codec);
	}

	@Override
	default Optional<ValueInput> child(String field)
	{
		return delegate().child(field);
	}

	@Override
	default ValueInput childOrEmpty(String field)
	{
		return delegate().childOrEmpty(field);
	}

	@Override
	default Optional<ValueInputList> childrenList(String field)
	{
		return delegate().childrenList(field);
	}

	@Override
	default ValueInputList childrenListOrEmpty(String field)
	{
		return delegate().childrenListOrEmpty(field);
	}

	@Override
	default <T> Optional<TypedInputList<T>> list(String field, Codec<T> codec)
	{
		return delegate().list(field, codec);
	}

	@Override
	default <T> TypedInputList<T> listOrEmpty(String field, Codec<T> codec)
	{
		return delegate().listOrEmpty(field, codec);
	}

	@Override
	default boolean getBooleanOr(String field, boolean value)
	{
		return delegate().getBooleanOr(field, value);
	}

	@Override
	default byte getByteOr(String field, byte value)
	{
		return delegate().getByteOr(field, value);
	}

	@Override
	default int getShortOr(String field, short value)
	{
		return delegate().getShortOr(field, value);
	}

	@Override
	default Optional<Integer> getInt(String field)
	{
		return delegate().getInt(field);
	}

	@Override
	default int getIntOr(String field, int value)
	{
		return delegate().getIntOr(field, value);
	}

	@Override
	default long getLongOr(String field, long value)
	{
		return delegate().getLongOr(field, value);
	}

	@Override
	default Optional<Long> getLong(String field)
	{
		return delegate().getLong(field);
	}

	@Override
	default float getFloatOr(String field, float value)
	{
		return delegate().getFloatOr(field, value);
	}

	@Override
	default double getDoubleOr(String field, double value)
	{
		return delegate().getDoubleOr(field, value);
	}

	@Override
	default Optional<String> getString(String field)
	{
		return delegate().getString(field);
	}

	@Override
	default String getStringOr(String field, String value)
	{
		return delegate().getStringOr(field, value);
	}

	@Override
	default Optional<int[]> getIntArray(String field)
	{
		return delegate().getIntArray(field);
	}

	@Override
	@Deprecated
	default Provider lookup()
	{
		return delegate().lookup();
	}
	
	
}
