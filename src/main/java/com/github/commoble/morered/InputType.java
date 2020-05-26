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
