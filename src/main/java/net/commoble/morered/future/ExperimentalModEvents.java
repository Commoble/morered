package net.commoble.morered.future;

import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.util.ConfigHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@EventBusSubscriber(modid=MoreRed.MODID, bus = Bus.MOD)
public class ExperimentalModEvents
{
	public static final ResourceKey<GameEvent> WIRE_UPDATE = ResourceKey.create(Registries.GAME_EVENT, MoreRed.getModRL("wire_update"));
	public static final DataMapType<Block, Wirer> WIRER_DATA_MAP = DataMapType.builder(MoreRed.getModRL("wirer"), Registries.BLOCK, Wirer.CODEC).build();
	
	public static final ExperimentalCommonConfig COMMON_CONFIG = ConfigHelper.register(MoreRed.MODID, ModConfig.Type.COMMON, ExperimentalCommonConfig::create);
	@SubscribeEvent
	public static void onModConstructed(FMLConstructModEvent event)
	{
		var wirerTypes = newRegistry(Wirer.REGISTRY_KEY);
		var gameEvents = defreg(Registries.GAME_EVENT);
		
		BiConsumer<ResourceKey<MapCodec<? extends Wirer>>, MapCodec<? extends Wirer>> registerWirer = (key,codec) -> wirerTypes.register(key.location().getPath(), () -> codec);
		
		registerWirer.accept(DefaultWirer.RESOURCE_KEY, DefaultWirer.CODEC);
		registerWirer.accept(CubeWirer.RESOURCE_KEY, CubeWirer.CODEC);
		registerWirer.accept(FloorWirer.RESOURCE_KEY, FloorWirer.CODEC);
		registerWirer.accept(WallFloorCeilingWirer.RESOURCE_KEY, WallFloorCeilingWirer.CODEC);
		registerWirer.accept(WireWirer.RESOURCE_KEY, WireWirer.CODEC);
		registerWirer.accept(WirePostWirer.RESOURCE_KEY, WirePostWirer.CODEC);
		registerWirer.accept(BitwiseGateWirer.RESOURCE_KEY, BitwiseGateWirer.CODEC);
		
		gameEvents.register(WIRE_UPDATE.location().getPath(), () -> new GameEvent(0));
	}
	
	@SubscribeEvent
	public static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event)
	{
		event.register(WIRER_DATA_MAP);
	}
	
	private static <T> DeferredRegister<T> newRegistry(ResourceKey<Registry<T>> key)
	{
		IEventBus modBus = ModList.get().getModContainerById(MoreRed.MODID).get().getEventBus();
		var defreg = DeferredRegister.create(key, MoreRed.MODID);
		defreg.makeRegistry(builder -> {});
		defreg.register(modBus);
		return defreg;
	}
	
	private static <T> DeferredRegister<T> defreg(ResourceKey<Registry<T>> key)
	{
		IEventBus modBus = ModList.get().getModContainerById(MoreRed.MODID).get().getEventBus();
		var defreg = DeferredRegister.create(key, MoreRed.MODID);
		defreg.register(modBus);
		return defreg;
	}
}
