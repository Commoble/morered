package net.commoble.morered.util;

import java.util.function.Function;
import java.util.function.Supplier;

public class Lambdas
{
	/**
	 * Converts a Supplier to a Function which accepts an argument but ignores it.
	 * Used to give Supplier-type method references to methods asking for Functions as parameters
	 * @param <T> Type of thing to supply
	 * @param supplier Supplier of the thing
	 * @return Function which accepts any input and runs the supplier instead
	 */
	public static <T> Function<?, T> ignoreInput(Supplier<T> supplier)
	{
		return object -> supplier.get();
	}
}
