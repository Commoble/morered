
package commoble.morered.client;

import commoble.morered.BlockRegistrar;
import commoble.morered.plate_blocks.InputState;
import commoble.morered.plate_blocks.LatchBlock;
import commoble.morered.plate_blocks.LogicFunction;
import commoble.morered.plate_blocks.LogicFunctions;
import commoble.morered.wire_post.WirePostBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ILightReader;

public class ColorHandlers
{
	
	public static final int NO_TINT = 0xFFFFFF;
	public static final int LIT = 0xFFFFFF;
	public static final int UNLIT = 0x560000;
	public static final int LIT_RED = LIT >> 16;
	public static final int UNLIT_RED = UNLIT >> 16;
	
	public static int getLogicFunctionBlockTint(BlockState state, ILightReader lightReader, BlockPos pos, int tintIndex)
	{
		return getLogicFunctionBlockStateTint(state, tintIndex);
	}
	
	public static int getLogicFunctionBlockStateTint(BlockState state, int tintIndex)
	{
		InputState input = InputState.getInput(state);
		
		return getLogicFunctionTint(tintIndex, input.a, input.b, input.c);
	}
	
	public static int getLogicFunctionBlockItemTint(ItemStack stack, int tintIndex)
	{
		Item item = stack.getItem();
		if (item instanceof BlockItem)
		{
			return getLogicFunctionBlockStateTint(((BlockItem)item).getBlock().getDefaultState(), tintIndex);
		}
		else
		{
			return NO_TINT;
		}
	}
	
	public static int getLogicFunctionTint(int tintIndex, boolean a, boolean b, boolean c)
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
	
	public static int getLatchBlockTint(BlockState state, ILightReader lightReader, BlockPos pos, int tintIndex)
	{
		return getLatchTint(state, tintIndex);
	}
	
	public static int getLatchItemTint(ItemStack stack, int tintIndex)
	{
		return getLatchTint(BlockRegistrar.LATCH.get().getDefaultState(), tintIndex);
	}
	
	public static int getLatchTint(BlockState state, int tintIndex)
	{
		if (tintIndex == LogicFunctions.SET_LATCH)
		{
			return state.get(LatchBlock.POWERED) && !state.get(LatchBlock.INPUT_C) ? LIT : UNLIT;
		}
		else if (tintIndex == LogicFunctions.UNSET_LATCH)
		{
			return !state.get(LatchBlock.POWERED) && !state.get(LatchBlock.INPUT_A) ? LIT : UNLIT;
		}
		else
		{
			InputState input = InputState.getInput(state);
			
			return getLogicFunctionTint(tintIndex, input.a, input.b, input.c);
		}
	}
	
	public static int getRedwirePostBlockTint(BlockState state, ILightReader lightReader, BlockPos pos, int tintIndex)
	{
		return getRedwirePostTint(state, tintIndex);
	}
	
	public static int getRedwirePostItemTint(ItemStack stack, int tintIndex)
	{
		return getRedwirePostTint(BlockRegistrar.REDWIRE_POST.get().getDefaultState(), tintIndex);
	}
	
	public static int getRedwirePostTint(BlockState state, int tintIndex)
	{
		if (tintIndex == 1)
		{
			int power = state.get(WirePostBlock.POWER);
			double lerpFactor = power / 15D;
			return ((int)MathHelper.lerp(lerpFactor, UNLIT_RED, LIT_RED)) << 16;
		}
		else
		{
			return NO_TINT;
		}
	}
}
