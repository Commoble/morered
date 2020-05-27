package com.github.commoble.morered;

import net.minecraft.block.Block;

@FunctionalInterface
public interface LogicGateBlockFactory
{	
	public LogicGatePlateBlock makeBlock(LogicFunction function, Block.Properties properties);
}
