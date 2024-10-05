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
	public static final DataMapType<Block, SignalSource> SIGNAL_SOURCE_DATA_MAP = DataMapType.builder(MoreRed.getModRL("signal_source"), Registries.BLOCK, SignalSource.CODEC).build();
	public static final DataMapType<Block, SignalTransmitter> SIGNAL_TRANSMITTER_DATA_MAP = DataMapType.builder(MoreRed.getModRL("signal_transmitter"), Registries.BLOCK, SignalTransmitter.CODEC).build();
	public static final DataMapType<Block, SignalReceiver> SIGNAL_RECEIVER_DATA_MAP = DataMapType.builder(MoreRed.getModRL("signal_receiver"), Registries.BLOCK, SignalReceiver.CODEC).build();
	
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
		
		gameEvents.register(WIRE_UPDATE.location().getPath(), () -> new GameEvent(0));
	}
	
	@SubscribeEvent
	public static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event)
	{
		event.register(SIGNAL_SOURCE_DATA_MAP);
		event.register(SIGNAL_TRANSMITTER_DATA_MAP);
		event.register(SIGNAL_RECEIVER_DATA_MAP);
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
