package commoble.morered.client;

import java.util.function.LongPredicate;

import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public class WireModelData implements IModelData, LongPredicate
{
	public static final ModelProperty<WireModelData> PROPERTY = new ModelProperty<>();
	
	/**
	 * The flags here are a bit pattern indicating a wire block's state and neighbor context
	 * first 6 bits [0-5] -- which interior faces the wire block has true wires attached to from blockitems
	 * 	the bits use the directional index, e.g. bit 0 = DOWN
	 * next 24 bits [6-29] -- indicates the connecting lines that should be rendered
	 * 	these use the 24 FaceRotation indices
	 * next 12 bits [30-41] -- indicates which connecting edges that should be rendered
	 * 	these use the 12 EdgeRotation indeces
	 */
	private final long flags;
	
	public WireModelData(long flags)
	{
		this.flags = flags;
	}

	@Override
	public boolean test(long i)
	{
		return ((1L << i) & this.flags) != 0;
	}

	@Override
	public boolean hasProperty(ModelProperty<?> prop)
	{
		return prop == PROPERTY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getData(ModelProperty<T> prop)
	{
		if (prop == PROPERTY)
			return (T)this;
		else
			return null;
	}

	@Override
	public <T> T setData(ModelProperty<T> prop, T data)
	{
		return data;
	}

}
