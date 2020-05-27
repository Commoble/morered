package com.github.commoble.morered;

import net.minecraft.block.Block;

public class InputType
{
	public final InputSide[] inputs;
	public final LogicGatePlateMaker plateMaker;
	
	public InputType(InputSide... inputs)
	{
		this.inputs = inputs;
		this.plateMaker = (properties, function) -> new LogicGatePlateBlock(function, properties)
		{
			// fillStateContainer in LogicGatePlateBlock needs to know which blockstate properties to use
			// but fillStateContainer gets called in the superconstructor, before any information about
			// our block is available.
			// The only safe way to handle this (aside from just making subclasses) is this
			// cursed closure class
			@Override
			public InputSide[] getInputSides()
			{
				return inputs;
			}
		};
	}
	
	public static interface LogicGatePlateMaker
	{
		public LogicGatePlateBlock makeBlock(LogicFunction function, Block.Properties properties);
	}
}
