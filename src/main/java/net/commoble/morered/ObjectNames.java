package net.commoble.morered;

import java.util.Arrays;

import net.minecraft.world.item.DyeColor;
import net.minecraft.Util;

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
	public static final String TWO_INPUT_AND_GATE = "two_input_and_gate";
	public static final String TWO_INPUT_NAND_GATE = "two_input_nand_gate";

	public static final String LATCH = "latch";
	public static final String PULSE_GATE = "pulse_gate";
	
	public static final String STONE_PLATE = "stone_plate";
	
	public static final String SOLDERING_TABLE = "soldering_table";
	
	public static final String WIRE_POST = "wire_post";
	public static final String REDWIRE_POST = "redwire_post";
	public static final String REDWIRE_RELAY = "redwire_relay";
	public static final String REDWIRE_JUNCTION = "redwire_junction";
	
	public static final String BUNDLED_CABLE_POST = "bundled_cable_post";
	public static final String BUNDLE_RELAY = "bundle_relay";
	public static final String BUNDLE_JUNCTION = "bundle_junction";
	
	public static final String WIRE = "wire";
	public static final String RED_ALLOY_WIRE = "red_alloy_wire";
	public static final String COLORED_NETWORK_CABLE = "colored_cable";
	public static final String POWERED_WIRE = "powered_wire";
	public static final String[] NETWORK_CABLES_BY_COLOR = Util.make(new String[16], array -> Arrays.setAll(array, i -> DyeColor.values()[i] + "_network_cable"));
	public static final String BUNDLED_NETWORK_CABLE = "bundled_network_cable";
	
	public static final String HEXIDECRUBROMETER = "hexidecrubrometer";
	
	public static final String BITWISE_GATE = "bitwise_gate";
	public static final String BITWISE_GATES = "bitwise_gates";
	public static final String SINGLE_INPUT_BITWISE_GATE = "single_input_bitwise_gate";
	public static final String TWO_INPUT_BITWISE_GATE = "two_input_bitwise_gate";
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
	
	// tags
	public static final String BUNDLED_CABLE_POSTS = "bundled_cable_posts";
	public static final String BUNDLED_CABLES = "bundled_cables";
	public static final String COLORED_CABLES = "colored_cables";
	public static final String CABLES = "cables";
	public static final String RED_ALLOY_WIRES = "red_alloy_wires";
	public static final String RED_ALLOYABLE_INGOTS = "red_alloyable_ingots";
	public static final String REDWIRE_POSTS = "redwire_posts";
	
	// capabilities
	public static final String POSTS_IN_CHUNK = "posts_in_chunk";
	public static final String VOXEL_CACHE = "voxel_cache";
	
	// data components
	public static final String SPOOLED_POST = "spooled_post";
	
	// recipe types
	public static final String SOLDERING_RECIPE = "soldering";
	
	// loot functions
	public static final String WIRE_COUNT = "set_wire_count";
	
	// model loaders
	public static final String WIRE_PARTS = "wire_parts";
	public static final String ROTATE_TINTS = "rotate_tints";
}
