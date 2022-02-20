package commoble.morered;

import net.minecraft.Util;
import net.minecraft.world.item.DyeColor;

import java.util.Arrays;

public class ObjectNames {
    // blocks and blockitems
    public static final String DIODE = "diode";
    public static final String NOT_GATE = "not_gate";
    public static final String NOR_GATE = "nor_gate";
    public static final String NAND_GATE = "nand_gate";
    public static final String OR_GATE = "or_gate";
    public static final String AND_GATE = "and_gate";
    public static final String XOR_GATE = "xor_gate";
    public static final String XNOR_GATE = "xnor_gate";
    public static final String MULTIPLEXER = "multiplexer";
    public static final String AND_2_GATE = "and_2_gate";
    public static final String NAND_2_GATE = "nand_2_gate";

    public static final String LATCH = "latch";

    public static final String STONE_PLATE = "stone_plate";

    public static final String GATECRAFTING_PLINTH = "gatecrafting_plinth";

    public static final String REDWIRE_POST = "redwire_post";
    public static final String REDWIRE_POST_PLATE = "redwire_post_plate";
    public static final String REDWIRE_POST_RELAY_PLATE = "redwire_post_relay_plate";

    public static final String BUNDLED_CABLE_POST = "bundled_cable_post";
    public static final String BUNDLED_CABLE_RELAY_PLATE = "bundled_cable_relay_plate";

    public static final String WIRE = "wire";
    public static final String RED_ALLOY_WIRE = "red_alloy_wire";
    public static final String COLORED_NETWORK_CABLE = "colored_network_cable";
    public static final String[] NETWORK_CABLES = Util.make(new String[16], array -> Arrays.setAll(array,
            i -> DyeColor.values()[i] + "_network_cable"));
    public static final String BUNDLED_NETWORK_CABLE = "bundled_network_cable";

    public static final String HEXIDECRUBROMETER = "hexidecrubrometer";

    public static final String BITWISE_LOGIC_PLATE = "bitwise_logic_plate";
    public static final String BITWISE_DIODE = "bitwise_diode";
    public static final String BITWISE_NOT_GATE = "bitwise_not_gate";
    public static final String BITWISE_OR_GATE = "bitwise_or_gate";
    public static final String BITWISE_AND_GATE = "bitwise_and_gate";
    public static final String BITWISE_XOR_GATE = "bitwise_xor_gate";
    public static final String BITWISE_XNOR_GATE = "bitwise_xnor_gate";

    // raw items
    public static final String RED_ALLOY_INGOT = "red_alloy_ingot";
    public static final String REDWIRE_SPOOL = "redwire_spool";
    public static final String BUNDLED_CABLE_SPOOL = "bundled_cable_spool";

    // capabilities
    public static final String POSTS_IN_CHUNK = "posts_in_chunk";

    // recipe types
    public static final String GATECRAFTING_RECIPE = "gatecrafting";

    // loot functions
    public static final String WIRE_COUNT = "set_wire_count";

    // model loaders
    public static final String WIRE_PARTS = "wire_parts";
    public static final String ROTATE_TINTS = "rotate_tints";
}
