package net.commoble.exmachina.internal.testcontent;

import java.util.function.Function;

import net.commoble.exmachina.internal.ExMachina;
import net.commoble.exmachina.internal.testcontent.client.GearRenderer;
import net.commoble.exmachina.internal.testcontent.client.MiniGearRenderer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid=ExMachina.MODID, bus=Bus.MOD)
public class TestContent
{
	private static final DeferredRegister.Blocks BLOCKS = defreg(DeferredRegister::createBlocks);
	private static final DeferredRegister.Items ITEMS = defreg(DeferredRegister::createItems);
	private static final DeferredRegister<BlockEntityType<?>> BLOCKENTITIES = defreg(Registries.BLOCK_ENTITY_TYPE);
	
//	public static final DeferredBlock<AxleBlock> AXLE_BLOCK = BLOCKS.registerBlock("axle", props -> new AxleBlock(props.noOcclusion()));
	public static final DeferredBlock<GearBlock> GEAR_BLOCK = BLOCKS.registerBlock("gear", props -> new GearBlock(props.noOcclusion()));
	public static final DeferredBlock<MiniGearBlock> MINIGEAR_BLOCK = BLOCKS.registerBlock("minigear", props -> new MiniGearBlock(props.noOcclusion()));
//	public static final DeferredItem<BlockItem> AXLE_ITEM = ITEMS.registerItem("axle", props -> new BlockItem(AXLE_BLOCK.get(), props));
	public static final DeferredItem<BlockItem> GEAR_ITEM = ITEMS.registerItem("gear", props -> new BlockItem(GEAR_BLOCK.get(), props));
	public static final DeferredItem<BlockItem> MINIGEAR = ITEMS.registerItem("minigear", props -> new BlockItem(MINIGEAR_BLOCK.get(), props));
//	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> AXLE_BLOCKENTITY = GenericBlockEntity.register(BLOCKENTITIES, "axle", AXLE_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> GEAR_BLOCKENTITY = GenericBlockEntity.register(BLOCKENTITIES, "gear", GEAR_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> MINIGEAR_BLOCKENTITY = GenericBlockEntity.register(BLOCKENTITIES, "minigear", MINIGEAR_BLOCK);
	
	@SubscribeEvent
	public static void onConstructMod(FMLConstructModEvent event)
	{
	}
	
	public static <T, R extends DeferredRegister<T>> R defreg(Function<String, R> registryFactory)
	{
		IEventBus modBus = ModList.get().getModContainerById(ExMachina.MODID).get().getEventBus();
		R defreg = registryFactory.apply(ExMachina.MODID);
		defreg.register(modBus);
		return defreg;
	}

	private static <T> DeferredRegister<T> defreg(ResourceKey<Registry<T>> key)
	{
		IEventBus modBus = ModList.get().getModContainerById(ExMachina.MODID).get().getEventBus();
		var defreg = DeferredRegister.create(key, ExMachina.MODID);
		defreg.register(modBus);
		return defreg;
	}
	
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent.Client event)
	{
//		event.createDatapackRegistryObjects(new RegistrySetBuilder()
//			.add(ExMachinaRegistries.MECHANICAL_COMPONENT, context -> {
//				MultipartMechanicalComponent gear = MultipartMechanicalComponent.builder(true);
//				for (Direction face : Direction.values())
//				{
//					List<RawConnection> connections = new ArrayList<>();
//					for (Direction orthagonalDirection : Direction.values())
//					{
//						if (orthagonalDirection.getAxis() == face.getAxis())
//							continue;
//						// what's the parity of this connection
//						// basically, either the connection inverts the parity or the axis does
//						// (i.e. if we go from positive to negative axis or vice-versa, we use Parity.POSITIVE
//						// otherwise parity.NEGATIVE
//						Parity parity = (face.ordinal() % 2 == 0) == (orthagonalDirection.ordinal() % 2 == 0)
//							? Parity.NEGATIVE
//							: Parity.POSITIVE;
//						connections.add(new RawConnection(
//							Optional.empty(),
//							NodeShape.ofSide(orthagonalDirection),
//							parity,
//							4));
//					}
//					connections.add(new RawConnection(
//						Optional.of(face),
//						NodeShape.ofSide(face.getOpposite()),
//						Parity.POSITIVE,
//						0));
//					gear.addApplyWhen(ApplyWhen.when(Case.create(GearBlock.PROPERTY_BY_DIRECTION.get(face), true), new RawNode(
//						NodeShape.ofSide(face), 0D, 0D, 0D, 5D, connections)));
//				}
//				context.register(ResourceKey.create(ExMachinaRegistries.MECHANICAL_COMPONENT, ExMachina.id("gear")), gear);
//				
//				VariantsMechanicalComponent axle = VariantsMechanicalComponent.builder(true);
//				for (Axis axis : Direction.Axis.VALUES)
//				{
//					axle.addVariant(AxleBlock.AXIS, axis,
//						new RawNode(NodeShape.ofCube(), 0,0,0, 0.02D, List.of(
//							new RawConnection(Optional.of(axis.getPositive()), NodeShape.ofSide(axis.getNegative()), Parity.POSITIVE, 0),
//							new RawConnection(Optional.of(axis.getNegative()), NodeShape.ofSide(axis.getPositive()), Parity.POSITIVE, 0)
//					)));
//							
//				}
//				context.register(ResourceKey.create(ExMachinaRegistries.MECHANICAL_COMPONENT, ExMachina.id("axle")), axle);
//				
//			})
//		);
	}
	
	@EventBusSubscriber(modid=ExMachina.MODID, bus=Bus.MOD, value=Dist.CLIENT)
	public static class ClientModEvents
	{
		@SubscribeEvent
		public static void onRegisterRenderers(RegisterRenderers event)
		{
//			event.registerBlockEntityRenderer(AXLE_BLOCKENTITY.get(), AxleRenderer::create);
			event.registerBlockEntityRenderer(GEAR_BLOCKENTITY.get(), GearRenderer::create);
			event.registerBlockEntityRenderer(MINIGEAR_BLOCKENTITY.get(), MiniGearRenderer::create);
		}
	}
}
