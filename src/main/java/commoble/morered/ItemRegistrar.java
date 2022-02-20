package commoble.morered;

import java.util.Arrays;

import commoble.morered.wire_post.WireSpoolItem;
import commoble.morered.wires.WireBlockItem;
import net.minecraft.Util;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistrar {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MoreRed.MODID);

    // using an objectholder here for the creative tab since the function gates are procedurally generated
    @ObjectHolder("morered:nor_gate")
    public static final BlockItem NOR_GATE = null;

    public static final CreativeModeTab CREATIVE_TAB = new CreativeModeTab(MoreRed.MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ItemRegistrar.NOR_GATE);
        }
    };

    public static final RegistryObject<WireSpoolItem> REDWIRE_SPOOL = ITEMS.register(ObjectNames.REDWIRE_SPOOL,
            () -> new WireSpoolItem(new Item.Properties().tab(CREATIVE_TAB).durability(64), BlockTags.bind("morered" +
                    ":redwire_posts")));
    public static final RegistryObject<Item> BUNDLED_CABLE_SPOOL = ITEMS.register(ObjectNames.BUNDLED_CABLE_SPOOL,
            () -> new WireSpoolItem(new Item.Properties().tab(CREATIVE_TAB).durability(64), BlockTags.bind("morered" +
                    ":bundled_cable_posts")));
    public static final RegistryObject<Item> RED_ALLOY_INGOT = ITEMS.register(ObjectNames.RED_ALLOY_INGOT,
            () -> new Item(new Item.Properties().tab(CREATIVE_TAB)));

    public static final RegistryObject<BlockItem> GATECRAFTING_PLINTH =
            registerBlockItem(ObjectNames.GATECRAFTING_PLINTH, BlockRegistrar.GATECRAFTING_PLINTH);
    public static final RegistryObject<BlockItem> REDWIRE_POST = registerBlockItem(ObjectNames.REDWIRE_POST,
            BlockRegistrar.REDWIRE_POST);
    public static final RegistryObject<BlockItem> REDWIRE_POST_PLATE =
            registerBlockItem(ObjectNames.REDWIRE_POST_PLATE, BlockRegistrar.REDWIRE_POST_PLATE);
    public static final RegistryObject<BlockItem> REDWIRE_POST_RELAY_PLATE =
            registerBlockItem(ObjectNames.REDWIRE_POST_RELAY_PLATE, BlockRegistrar.REDWIRE_POST_RELAY_PLATE);
    public static final RegistryObject<BlockItem> BUNDLED_CABLE_POST =
            registerBlockItem(ObjectNames.BUNDLED_CABLE_POST, BlockRegistrar.BUNDLED_CABLE_POST);
    public static final RegistryObject<BlockItem> BUNDLED_CABLE_RELAY_PLATE =
            registerBlockItem(ObjectNames.BUNDLED_CABLE_RELAY_PLATE, BlockRegistrar.BUNDLED_CABLE_RELAY_PLATE);
    public static final RegistryObject<BlockItem> HEXIDECRUBROMETER = registerBlockItem(ObjectNames.HEXIDECRUBROMETER
            , BlockRegistrar.HEXIDECRUBROMETER);

    public static final RegistryObject<BlockItem> STONE_PLATE = registerBlockItem(ObjectNames.STONE_PLATE,
            BlockRegistrar.STONE_PLATE);
    public static final RegistryObject<BlockItem> LATCH = registerBlockItem(ObjectNames.LATCH, BlockRegistrar.LATCH);

    public static final RegistryObject<BlockItem> RED_ALLOY_WIRE = ITEMS.register(ObjectNames.RED_ALLOY_WIRE,
            () -> new WireBlockItem(BlockRegistrar.RED_ALLOY_WIRE.get(), new Item.Properties().tab(CREATIVE_TAB)));

    @SuppressWarnings("unchecked")
    public static final RegistryObject<WireBlockItem>[] NETWORK_CABLES =
            Util.make((RegistryObject<WireBlockItem>[]) new RegistryObject[16], array ->
                    Arrays.setAll(array, i -> ITEMS.register(ObjectNames.NETWORK_CABLES[i],
                            () -> new WireBlockItem(BlockRegistrar.NETWORK_CABLES[i].get(),
                                    new Item.Properties().tab(CREATIVE_TAB)))));

    public static final RegistryObject<WireBlockItem> BUNDLED_NETWORK_CABLE =
            ITEMS.register(ObjectNames.BUNDLED_NETWORK_CABLE,
                    () -> new WireBlockItem(BlockRegistrar.BUNDLED_NETWORK_CABLE.get(),
                            new Item.Properties().tab(CREATIVE_TAB)));


    public static final RegistryObject<BlockItem> registerBlockItem(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(CREATIVE_TAB)));
    }
}
