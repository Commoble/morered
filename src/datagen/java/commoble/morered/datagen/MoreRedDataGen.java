package commoble.morered.datagen;

import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.datagen.JsonDataProvider.ResourceType;
import commoble.morered.wires.WireBlockItem;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod("morered-datagen")
@EventBusSubscriber(modid="morered-datagen", bus=Bus.MOD)
public class MoreRedDataGen
{
	@SubscribeEvent
	static void onGatherData(GatherDataEvent event)
	{
		// FMLCommonSetup doesn't run during run data, have to invoke this here as well
		MoreRed.registerToVanillaRegistries();
		
		DataGenerator generator = event.getGenerator();
		generator.addProvider(new ColoredCablesDataProvider(generator, event.getExistingFileHelper()));
		
		generator.addProvider(new JsonDataProvider<TagDefinition>(generator, ResourceType.DATA, "tags/items", TagDefinition.CODEC)
			.with(new ResourceLocation(MoreRed.MODID, "colored_network_cables"), Util.make(TagDefinition.builder(), builder ->
			{
				for (RegistryObject<WireBlockItem> item : ItemRegistrar.NETWORK_CABLES)
					builder.withObject(item.get());
			}))
			.with(new ResourceLocation(MoreRed.MODID, "network_cables"), TagDefinition.builder()
				.withObject(ItemRegistrar.BUNDLED_NETWORK_CABLE.get())
				.withTag("morered:colored_network_cables")));
	}
}
