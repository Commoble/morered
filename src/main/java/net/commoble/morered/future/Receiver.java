package net.commoble.morered.future;

import net.minecraft.world.level.LevelAccessor;

@FunctionalInterface
public interface Receiver
{
	public abstract void accept(LevelAccessor level, int power);
}
