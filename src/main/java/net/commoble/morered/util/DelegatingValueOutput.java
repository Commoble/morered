package net.commoble.morered.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.storage.ValueOutput;

public interface DelegatingValueOutput extends ValueOutput
{
	public abstract ValueOutput delegate();

	@Override
	public default <T> void store(String field, Codec<T> codec, T value)
	{
		this.delegate().store(field, codec, value);
	}

	@Override
	public default <T> void storeNullable(String field, Codec<T> codec, T value)
	{
		this.delegate().storeNullable(field, codec, value);
	}

	@Deprecated
	@Override
	public default <T> void store(MapCodec<T> codec, T value)
	{
		this.delegate().store(codec, value);
	}

	@Override
	public default void putBoolean(String field, boolean value)
	{
		this.delegate().putBoolean(field, value);
	}

	@Override
	public default void putByte(String field, byte value)
	{
		this.delegate().putByte(field, value);
	}

	@Override
	public default void putShort(String field, short value)
	{
		this.delegate().putShort(field, value);
	}

	@Override
	public default void putInt(String field, int value)
	{
		this.delegate().putInt(field, value);
	}

	@Override
	public default void putLong(String field, long value)
	{
		this.delegate().putLong(field, value);
	}

	@Override
	public default void putFloat(String field, float value)
	{
		this.delegate().putFloat(field, value);
	}

	@Override
	public default void putDouble(String field, double value)
	{
		this.delegate().putDouble(field, value);
	}

	@Override
	public default void putString(String field, String value)
	{
		this.delegate().putString(field, value);
	}

	@Override
	public default void putIntArray(String field, int[] value)
	{
		this.delegate().putIntArray(field, value);
	}

	@Override
	public default ValueOutput child(String field)
	{
		return this.delegate().child(field);
	}

	@Override
	public default ValueOutputList childrenList(String field)
	{
		return this.delegate().childrenList(field);
	}

	@Override
	public default <T> TypedOutputList<T> list(String field, Codec<T> codec)
	{
		return this.delegate().list(field, codec);
	}

	@Override
	public default void discard(String field)
	{
		this.delegate().discard(field);
	}

	@Override
	public default boolean isEmpty()
	{
		return delegate().isEmpty();
	}

}
