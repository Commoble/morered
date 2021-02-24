package commoble.morered;

import java.util.Arrays;

import net.minecraft.item.DyeColor;
import net.minecraft.util.Util;

public class ObjectNames
{
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
	
	public static final String WIRE = "wire";
	public static final String RED_ALLOY_WIRE = "red_alloy_wire";
	public static final String COLORED_NETWORK_CABLE = "colored_network_cable";
	public static final String[] NETWORK_CABLES = Util.make(new String[16], array -> Arrays.setAll(array, i -> DyeColor.values()[i] + "_network_cable"));
	public static final String BUNDLED_NETWORK_CABLE = "bundled_network_cable";
	
	public static final String HEXIDECRUBROMETER = "hexidecrubrometer";

	// raw items
	public static final String RED_ALLOY_INGOT = "red_alloy_ingot";
	public static final String REDWIRE_SPOOL = "redwire_spool";
	
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
