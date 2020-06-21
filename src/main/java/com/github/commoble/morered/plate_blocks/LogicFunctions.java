package com.github.commoble.morered.plate_blocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class LogicFunctions
{
	public static final Int2ObjectMap<LogicFunction> TINTINDEXES = new Int2ObjectOpenHashMap<>();
	
	// we explicitly enumerate these because the model jsons' tintindexes need literal ints and I want to see which ints are for what
	// particles have tintindex 0 for some reason, start counting at 1 so we don't tint particles
	public static final LogicFunction FALSE = registerTintIndex(1, (a,b,c) -> false);
	public static final LogicFunction TRUE = registerTintIndex(2, (a,b,c) -> true);
	public static final LogicFunction INPUT_A = registerTintIndex(3, (a,b,c) -> a);
	public static final LogicFunction INPUT_B = registerTintIndex(4, (a,b,c) -> b);
	public static final LogicFunction INPUT_C = registerTintIndex(5, (a,b,c) -> c);
	public static final LogicFunction NOT_A = registerTintIndex(6, (a,b,c) -> !a);
	public static final LogicFunction NOT_B = registerTintIndex(7, (a,b,c) -> !b);
	public static final LogicFunction NOT_C = registerTintIndex(8, (a,b,c) -> !c);
	public static final LogicFunction AND = registerTintIndex(9, (a,b,c) -> a && b && c);
	public static final LogicFunction NAND = registerTintIndex(10, (a,b,c) -> !(a && b && c));
	public static final LogicFunction OR = registerTintIndex(11, (a,b,c) -> a || b || c);
	public static final LogicFunction NOR = registerTintIndex(12, (a,b,c) -> !(a || b || c));
	// we omit B in the xor functions because the xor/xnor logic plates don't have a B input 
	public static final LogicFunction XOR_AC = registerTintIndex(13, (a,b,c) -> a ^ c);
	public static final LogicFunction XNOR_AC = registerTintIndex(14, (a,b,c) -> !(a^c));
	public static final LogicFunction MULTIPLEX = registerTintIndex(15, (a,b,c) -> b ? c : a);
	// these two are used for intermediary component tinting in the XOR/XNOR gate models
	public static final LogicFunction A_NOR_A_NOR_C = registerTintIndex(16, (a,b,c) -> !(a || !(a || c)));
	public static final LogicFunction C_NOR_A_NOR_C = registerTintIndex(17, (a,b,c) -> !(c || !(a || c)));
	public static final int SET_LATCH = 18;	// state-memory blocks don't use logic functions
	public static final int UNSET_LATCH = 19;
	// two input AND gate (does not use B input)
	public static final LogicFunction AND_2 = registerTintIndex(20, (a,b,c) -> a && c);
	public static final LogicFunction NAND_2 = registerTintIndex(21, (a,b,c) -> !(a && c));

	public static LogicFunction registerTintIndex(int index, LogicFunction function)
	{
		TINTINDEXES.put(index, function);
		return function;
	}
}
