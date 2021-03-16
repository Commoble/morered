package commoble.morered.datagen;

import java.util.Arrays;

import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.datagen.JsonDataProvider.ResourceType;
import commoble.morered.wires.WireBlockItem;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.tags.ItemTags;
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
	@SuppressWarnings("unchecked")
	public static final INamedTag<Item>[] WOOL_TAGS = Util.make((INamedTag<Item>[])new INamedTag[16], array->
		Arrays.setAll(array, i -> ItemTags.makeWrapperTag("forge:wools/" + DyeColor.values()[i].getTranslationKey())));
	
	
	@SubscribeEvent
	static void onGatherData(GatherDataEvent event)
	{
		// FMLCommonSetup doesn't run during run data, have to invoke this here as well
		MoreRed.registerToVanillaRegistries();
		
		DataGenerator generator = event.getGenerator();
		generator.addProvider(new ColoredCablesDataProvider(generator, event.getExistingFileHelper()));
		
		JsonDataProvider<TagDefinition> tagProvider = new JsonDataProvider<TagDefinition>(generator, ResourceType.DATA, "tags/items", TagDefinition.CODEC);
		// make tag for colored network cables
		tagProvider.with(new ResourceLocation(MoreRed.MODID, "colored_network_cables"), Util.make(TagDefinition.builder(), builder ->
			{
				for (RegistryObject<WireBlockItem> item : ItemRegistrar.NETWORK_CABLES)
					builder.withObject(item.get());
			}));

		// make tags for wool blocks
		for (int i=0; i<16; i++)
		{
			tagProvider.with(WOOL_TAGS[i].getName(), TagDefinition.builder().with(String.format("minecraft:%s_wool", DyeColor.values()[i].getString())));
		}
		
		generator.addProvider(tagProvider);
	}
}
