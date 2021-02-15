package commoble.morered.datagen;

import commoble.morered.MoreRed;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
		event.getGenerator().addProvider(new ColoredCablesDataProvider(generator, event.getExistingFileHelper()));
	}
}
