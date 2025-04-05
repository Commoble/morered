package net.commoble.morered;

import java.util.Arrays;

import net.minecraft.Util;
import net.minecraft.world.item.DyeColor;

public class Names
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

	public static final String CABLE_JUNCTION = "cable_junction";
	public static final String CABLE_RELAY = "cable_relay";
	
	public static final String WIRE = "wire";
	public static final String RED_ALLOY_WIRE = "red_alloy_wire";
	public static final String COLORED_CABLE = "colored_cable";
	public static final String POWERED_WIRE = "powered_wire";
	public static final String[] COLORED_CABLES_BY_COLOR = Util.make(new String[16], array -> Arrays.setAll(array, i -> DyeColor.values()[i] + "_cable"));
	public static final String BUNDLED_CABLE = "bundled_cable";
	
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
	
	public static final String TUBE = "tube";
	public static final String SHUNT = "shunt";
	public static final String LOADER = "loader";
	public static final String REDSTONE_TUBE = "redstone_tube";
	public static final String EXTRACTOR = "extractor";
	public static final String FILTER = "filter";
	public static final String MULTIFILTER = "multifilter";
	public static final String OSMOSIS_FILTER = "osmosis_filter";
	public static final String OSMOSIS_SLIME = "osmosis_slime";
	public static final String DISTRIBUTOR = "distributor";
	public static final String[] COLORED_TUBE_NAMES = Arrays.stream(DyeColor.values()).map(color -> color.toString() + "_tube").toArray(String[]::new);
	
	public static final String AXLE = "axle";
	public static final String GEAR = "gear";
	public static final String GEARSHIFTER = "gearshifter";
	public static final String WINDCATCHER = "windcatcher";
	public static final String AIRFOIL = "airfoil";

	// raw items
	public static final String RED_ALLOY_INGOT = "red_alloy_ingot";
	public static final String REDWIRE_SPOOL = "redwire_spool";
	public static final String BUNDLED_CABLE_SPOOL = "bundled_cable_spool";
	public static final String PLIERS = "pliers";
	
	// tags
	public static final String CABLE_POSTS = "cable_posts";
	public static final String BUNDLED_CABLES = "bundled_cables";
	public static final String COLORED_CABLES = "colored_cables";
	public static final String CABLES = "cables";
	public static final String RED_ALLOY_WIRES = "red_alloy_wires";
	public static final String RED_ALLOYABLE_INGOTS = "red_alloyable_ingots";
	public static final String REDWIRE_POSTS = "redwire_posts";
	public static final String COLORED_TUBES = "colored_tubes";
	public static final String TUBES = "tubes";
	public static final String AXLES = "axles";
	public static final String GEARS = "gears";
	public static final String GEARSHIFTERS = "gearshifters";
	public static final String WINDCATCHERS = "windcatchers";
	public static final String AIRFOILS = "airfoils";
	
	public static final String SUPPORTED_STRIPPED_LOGS = "supported_stripped_logs";
	
	// capabilities
	public static final String POSTS_IN_CHUNK = "posts_in_chunk";
	public static final String TUBES_IN_CHUNK = "tubes_in_chunk";
	public static final String VOXEL_CACHE = "voxel_cache";
	
	// data components
	public static final String SPOOLED_POST = "spooled_post";
	public static final String PLIERED_TUBE = "pliered_tube";
	public static final String WINDCATCHER_COLORS = "windcatcher_colors";
	
	// recipe types
	public static final String SOLDERING_RECIPE = "soldering";
	public static final String WINDCATCHER_DYE = "windcatcher_dye";
	
	// loot functions
	public static final String WIRE_COUNT = "set_wire_count";
	
	// model loaders
	public static final String WIRE_PARTS = "wire_parts";
	public static final String ROTATE_TINTS = "rotate_tints";
	public static final String LOGIC_GATE = "logic_gate";
}
