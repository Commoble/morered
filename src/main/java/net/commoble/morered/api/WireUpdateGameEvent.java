package net.commoble.morered.api;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;

public class WireUpdateGameEvent
{
	public static final ResourceKey<GameEvent> RESOURCE_KEY = ResourceKey.create(Registries.GAME_EVENT, MoreRed.getModRL("wire_update"));

	public static void scheduleWireUpdate(LevelAccessor level, BlockPos pos)
	{
		level.gameEvent(RESOURCE_KEY, pos, GameEvent.Context.of(null, null));
	}
}
