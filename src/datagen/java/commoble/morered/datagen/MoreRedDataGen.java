package commoble.morered.datagen;

import java.util.Arrays;

import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.datagen.JsonDataProvider.ResourceType;
import commoble.morered.wires.WireBlockItem;
import net.minecraft.Util;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = "morered-datagen", bus = Bus.MOD)
public class MoreRedDataGen {
    @SuppressWarnings("unchecked")
    public static final Tag.Named<Item>[] WOOL_TAGS = Util.make((Tag.Named<Item>[]) new Tag.Named[16], array ->
            Arrays.setAll(array, i -> ItemTags.bind("forge:wools/" + DyeColor.values()[i].getSerializedName())));


    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        // FMLCommonSetup doesn't run during run data, have to invoke this here as well
        MoreRed.registerToVanillaRegistries();

        DataGenerator generator = event.getGenerator();
        generator.addProvider(new ColoredCablesDataProvider(generator, event.getExistingFileHelper()));

        JsonDataProvider<TagDefinition> tagProvider = new JsonDataProvider<TagDefinition>(generator,
                ResourceType.DATA, "tags/items", TagDefinition.CODEC);
        // make tag for colored network cables
        tagProvider.with(new ResourceLocation(MoreRed.MODID, "colored_network_cables"),
                Util.make(TagDefinition.builder(), builder ->
                {
                    for (RegistryObject<WireBlockItem> item : ItemRegistrar.NETWORK_CABLES)
                        builder.withObject(item.get());
                }));

        // make tags for wool blocks
        for (int i = 0; i < 16; i++) {
            tagProvider.with(WOOL_TAGS[i].getName(), TagDefinition.builder().with(String.format("minecraft:%s_wool",
                    DyeColor.values()[i].getSerializedName())));
        }

        generator.addProvider(tagProvider);
    }
}
