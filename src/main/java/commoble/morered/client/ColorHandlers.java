
package commoble.morered.client;

import commoble.morered.BlockRegistrar;
import commoble.morered.plate_blocks.InputState;
import commoble.morered.plate_blocks.LatchBlock;
import commoble.morered.plate_blocks.LogicFunction;
import commoble.morered.plate_blocks.LogicFunctions;
import commoble.morered.wire_post.AbstractWirePostBlock;
import commoble.morered.wires.Edge;
import commoble.morered.wires.WireTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockDisplayReader;

public class ColorHandlers
{
	
	public static final int NO_TINT = 0xFFFFFF;
	public static final int LIT = 0xFFFFFF;
	public static final int UNLIT = 0x560000;
	public static final int LIT_RED = LIT >> 16;
	public static final int UNLIT_RED = UNLIT >> 16;
	
	public static int getLogicFunctionBlockTint(BlockState state, IBlockDisplayReader lightReader, BlockPos pos, int tintIndex)
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
	
	public static int getLatchBlockTint(BlockState state, IBlockDisplayReader lightReader, BlockPos pos, int tintIndex)
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
	
	public static int getRedwirePostBlockTint(BlockState state, IBlockDisplayReader lightReader, BlockPos pos, int tintIndex)
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
			int power = state.get(AbstractWirePostBlock.POWER);
			double lerpFactor = power / 15D;
			return ((int)MathHelper.lerp(lerpFactor, UNLIT_RED, LIT_RED)) << 16;
		}
		else
		{
			return NO_TINT;
		}
	}
	
	public static int getRedAlloyWireBlockTint(BlockState state, IBlockDisplayReader world, BlockPos pos, int tintIndex)
	{
		if (tintIndex < 0 || tintIndex > 18) // no tint specified / unused
			return NO_TINT;
		if (tintIndex == 0) // reserved for particle, particle tint is hardcoded to 0
			return UNLIT;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof WireTileEntity)
		{
			WireTileEntity wire = (WireTileEntity)te;
			if (tintIndex < 7) // range is [1,6], indicating a face tint
			{
				int side = tintIndex-1;
				int power = wire.getPower(side);
				double lerpFactor = power/32D;
				return ((int)MathHelper.lerp(lerpFactor, UNLIT_RED, LIT_RED)) << 16;
			}
			else // range is [7,18], indicating an edge tint
			{
				// average litness from neighbor wires
				int edgeIndex = tintIndex - 7;
				Edge edge = Edge.values()[edgeIndex];
				Direction directionA = edge.sideA;
				BlockPos neighborPosA = pos.offset(directionA);
				TileEntity neighborTileA = world.getTileEntity(neighborPosA);
				if (neighborTileA instanceof WireTileEntity)
				{
					WireTileEntity neighborWireA = (WireTileEntity)neighborTileA;
					Direction directionB = edge.sideB;
					BlockPos neighborPosB = pos.offset(directionB);
					TileEntity neighborTileB = world.getTileEntity(neighborPosB);
					if (neighborTileB instanceof WireTileEntity)
					{
						WireTileEntity neighborWireB = (WireTileEntity)neighborTileB;
						double powerA = neighborWireA.getPower(directionB);
						double powerB = neighborWireB.getPower(directionA);
						double averagePower = (powerA + powerB)/2D;
						double lerpFactor = averagePower/32D;
						return ((int)MathHelper.lerp(lerpFactor, UNLIT_RED, LIT_RED)) << 16;
					}
				}
			}
		}
		return NO_TINT;
	}
}
