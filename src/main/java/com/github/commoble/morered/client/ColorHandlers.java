
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
	
	public static int getLogicGateTint(BlockState state, ILightReader lightReader, BlockPos pos, int tintIndex)
	{
		InputState input = InputState.getInput(state);
		
		return getLogicGateTint(tintIndex, input.a, input.b, input.c);
	}
	
	public static int getLogicGateTint(int tintIndex, boolean a, boolean b, boolean c)
	{
		if (tintIndex < 1) // particles have tintindex 0?, unspecified faces have tintindex -1
		{
			return NO_TINT;
		}
		
		// tintindexes are enumerated in LogicFunctions
		// each specified function has a specific tint index associated with it,
		// so a redstone overlay on a model can be determined to be "on" or "off" based on
		// the block's input state.
		// the indexes aren't in any rational order, refer to the IDs
		// in LogicFunctions when setting the indexes in the model jsons
		LogicFunction logicFunction = LogicFunctions.TINTINDEXES.getOrDefault(tintIndex, LogicFunctions.FALSE);
		return logicFunction.apply(a, b, c) ? LIT : UNLIT;
	}
}
