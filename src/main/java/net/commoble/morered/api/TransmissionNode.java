package net.commoble.morered.api;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;

public record TransmissionNode(
	Set<Direction> powerReaders,
	Set<Face> connectableNodes,
	BiFunction<LevelAccessor,Integer,Map<Direction, SignalStrength>> graphListener)
{
}
