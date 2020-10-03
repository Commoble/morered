package commoble.morered.util;

import java.util.Optional;

public class ClassHelper
{
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> as(Object thing, Class<T> clazz)
	{
		return clazz.isInstance(thing) ? Optional.of((T) thing) : Optional.empty();
	}
}