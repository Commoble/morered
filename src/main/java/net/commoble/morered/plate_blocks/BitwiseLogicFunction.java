package net.commoble.morered.plate_blocks;

public interface BitwiseLogicFunction
{
	/**
	 * 
	 * @param a The first input for a gate, 90 degrees clockwise from the output, 16 bits where white = LSB
	 * @param b The second input for a gate, 180 degrees clockwise from the output, 16 bits where white = LSB
	 * @param c The third input for a gate, 270 degrees clockwise from the output, 16 bits where white = LSB
	 * @return integer bitflag with 16 bits after applying bitwise operator to the inputs
	 */
	public int apply(int a, int b, int c);
}
