package net.commoble.morered.future;

import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.minecraft.core.Direction;

public record TransmissionNode(
	Set<Direction> powerReaders,
	Set<Face> connectableNodes,
	Int2ObjectFunction<Set<Direction>> graphListener)
{
}
