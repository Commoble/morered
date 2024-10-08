package net.commoble.morered.api.internal;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.WireUpdateGameEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid=MoreRed.MODID)
public class ExperimentalGameEvents
{
	@SubscribeEvent
	public static void onVanillaGameEvent(VanillaGameEvent event)
	{
		// called when a block update occurs at a given position (including when a blockstate change occurs at that position)
		// if the blockstate changed, the event's given state is the new blockstate
		LevelAccessor level = event.getLevel();
		
		if (level instanceof ServerLevel serverLevel && event.getVanillaEvent().is(WireUpdateGameEvent.RESOURCE_KEY))
		{
			WireUpdateBuffer.get(serverLevel).enqueue(BlockPos.containing(event.getEventPosition()));
		}
	}
	
	@SubscribeEvent
	public static void endOfLevelTick(LevelTickEvent.Post event)
	{
		if (event.getLevel() instanceof ServerLevel serverLevel)
			WireUpdateBuffer.get(serverLevel).tick(serverLevel);
	}
}
