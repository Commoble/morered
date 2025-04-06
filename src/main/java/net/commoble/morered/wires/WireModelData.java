package net.commoble.morered.wires;

import net.neoforged.neoforge.model.data.ModelProperty;

public final class WireModelData
{
	private WireModelData() {}; // static member holder
	
	public static final ModelProperty<Long> PROPERTY = new ModelProperty<>();

	/**
	 * The flags here are a bit pattern indicating a wire block's state and neighbor context
	 * first 6 bits [0-5] -- which interior faces the wire block has true wires attached to from blockitems
	 * 	the bits use the directional index, e.g. bit 0 = DOWN
	 * next 24 bits [6-29] -- indicates the connecting lines that should be rendered
	 * 	these use the 24 FaceRotation indices
	 * next 12 bits [30-41] -- indicates which connecting edges that should be rendered
	 * 	these use the 12 EdgeRotation indeces
	 */
	public static boolean test(long flags, int bitIndex)
	{
		return ((1L << bitIndex) & flags) != 0;
	}
}