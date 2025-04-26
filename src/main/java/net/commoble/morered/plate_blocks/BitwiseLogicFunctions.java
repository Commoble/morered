package net.commoble.morered.plate_blocks;

public class BitwiseLogicFunctions
{
	public static final int SIXTEEN_BITS = 0xFFFF;
	public static final BitwiseLogicFunction FALSE = (a,b,c) -> 0;
	public static final BitwiseLogicFunction TRUE = (a,b,c) -> SIXTEEN_BITS;
	public static final BitwiseLogicFunction INPUT_B = (a,b,c) -> b & SIXTEEN_BITS;
	public static final BitwiseLogicFunction NOT_B = (a,b,c) -> (~b) & SIXTEEN_BITS;
	public static final BitwiseLogicFunction OR = (a,b,c) -> (a | b | c) & SIXTEEN_BITS;
	public static final BitwiseLogicFunction AND_2 = (a,b,c) -> (a & c) & SIXTEEN_BITS;
	public static final BitwiseLogicFunction XOR_AC = (a,b,c) -> (a ^ c) & SIXTEEN_BITS;
	public static final BitwiseLogicFunction XNOR_AC = (a,b,c) -> (~(a ^ c)) & SIXTEEN_BITS;
	public static final BitwiseLogicFunction MULTIPLEX = (a,b,c) -> ((c & b) + (a & ~b)) & SIXTEEN_BITS;
}
