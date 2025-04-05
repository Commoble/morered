package net.commoble.morered.util;

import net.commoble.exmachina.api.Parity;
import net.minecraft.core.Direction;

// TODO move these to Ex Machina when it's more convenient
public class ExtraMachina
{
	public static Parity swapParity(Direction a, Direction b)
	{
		return a.getAxisDirection() == b.getAxisDirection() ? Parity.NEGATIVE : Parity.POSITIVE;
	}
}
