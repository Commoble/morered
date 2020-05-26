package com.github.commoble.morered.client;

import com.github.commoble.morered.InputState;
import com.github.commoble.morered.LogicFunction;
import com.github.commoble.morered.LogicFunctions;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;

public class BlockColorHandlers
{
	
	public static final int NO_TINT = 0xFFFFFF;
	public static final int LIT = 0xFFFFFF;
	public static final int UNLIT = 0x560000;
	
	public static int getTint(int tintIndex, boolean a, boolean b, boolean c)
	{
		if (tintIndex < 1) // particles have tintindex 0?, unspecified faces have tintindex -1
		{
			return NO_TINT;
		}
		
		LogicFunction logicFunction = LogicFunctions.TINTINDEXES.getOrDefault(tintIndex, LogicFunctions.FALSE);
		return logicFunction.apply(a, b, c) ? LIT : UNLIT;
	}
	
	public static int getLogicGateTint(BlockState state, ILightReader lightReader, BlockPos pos, int tintIndex)
	{
		InputState input = InputState.getPreviousState(state);
		
		return getTint(tintIndex, input.a, input.b, input.c);
	}
}
