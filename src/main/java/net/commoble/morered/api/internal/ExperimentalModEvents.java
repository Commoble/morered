package net.commoble.morered.api.internal;

import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.CubeSource;
import net.commoble.morered.api.DefaultSource;
import net.commoble.morered.api.FloorSource;
import net.commoble.morered.api.NoneSource;
import net.commoble.morered.api.SignalReceiver;
import net.commoble.morered.api.SignalSource;
import net.commoble.morered.api.SignalTransmitter;
import net.commoble.morered.api.WallFloorCeilingSource;
import net.commoble.morered.api.WireUpdateGameEvent;
import net.commoble.morered.util.ConfigHelper;
import net.commoble.morered.wires.BitwiseGateReceiver;
import net.commoble.morered.wires.BitwiseGateSource;
import net.commoble.morered.wires.WirePostWirer;
import net.commoble.morered.wires.WireTransmitter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@EventBusSubscriber(modid=MoreRed.MODID, bus = Bus.MOD)
public class ExperimentalModEvents
{
	public static final ExperimentalCommonConfig COMMON_CONFIG = ConfigHelper.register(MoreRed.MODID, ModConfig.Type.COMMON, ExperimentalCommonConfig::create);
	
	@SubscribeEvent
	public static void onModConstructed(FMLConstructModEvent event)
	{
		var sources = newRegistry(SignalSource.REGISTRY_KEY);
		var transmitters = newRegistry(SignalTransmitter.REGISTRY_KEY);
		var receivers = newRegistry(SignalReceiver.REGISTRY_KEY);
		var gameEvents = defreg(Registries.GAME_EVENT);
		
		BiConsumer<ResourceKey<MapCodec<? extends SignalSource>>, MapCodec<? extends SignalSource>> registerSource = (key,codec) -> sources.register(key.location().getPath(), () -> codec);
		BiConsumer<ResourceKey<MapCodec<? extends SignalTransmitter>>, MapCodec<? extends SignalTransmitter>> registerTransmitter = (key,codec) -> transmitters.register(key.location().getPath(), () -> codec);
		BiConsumer<ResourceKey<MapCodec<? extends SignalReceiver>>, MapCodec<? extends SignalReceiver>> registerReceiver = (key,codec) -> receivers.register(key.location().getPath(), () -> codec);

		registerSource.accept(NoneSource.RESOURCE_KEY, NoneSource.CODEC);
		registerSource.accept(DefaultSource.RESOURCE_KEY, DefaultSource.CODEC);
		registerSource.accept(CubeSource.RESOURCE_KEY, CubeSource.CODEC);
		registerSource.accept(FloorSource.RESOURCE_KEY, FloorSource.CODEC);
		registerSource.accept(WallFloorCeilingSource.RESOURCE_KEY, WallFloorCeilingSource.CODEC);
		registerSource.accept(BitwiseGateSource.RESOURCE_KEY, BitwiseGateSource.CODEC);
		registerTransmitter.accept(WireTransmitter.RESOURCE_KEY, WireTransmitter.CODEC);
		registerTransmitter.accept(WirePostWirer.RESOURCE_KEY, WirePostWirer.CODEC);
		registerReceiver.accept(BitwiseGateReceiver.RESOURCE_KEY, BitwiseGateReceiver.CODEC);
		
		gameEvents.register(WireUpdateGameEvent.RESOURCE_KEY.location().getPath(), () -> new GameEvent(0));
	}
	
	@SubscribeEvent
	public static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event)
	{
		event.register(SignalSource.DATA_MAP_TYPE);
		event.register(SignalTransmitter.DATA_MAP_TYPE);
		event.register(SignalReceiver.DATA_MAP_TYPE);
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
