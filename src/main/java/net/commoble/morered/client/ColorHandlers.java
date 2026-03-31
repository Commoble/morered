package net.commoble.morered.client;

import java.util.List;
import java.util.stream.IntStream;

import net.commoble.morered.plate_blocks.AlternatorBlock;
import net.commoble.morered.plate_blocks.InputState;
import net.commoble.morered.plate_blocks.LatchBlock;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.LogicFunctions;
import net.commoble.morered.wire_post.AbstractPoweredWirePostBlock;
import net.commoble.morered.wires.Edge;
import net.commoble.morered.wires.PoweredWireBlockEntity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ColorHandlers
{
	
	public static final int NO_TINT = 0xFFFFFFFF;
	public static final int LIT = 0xFFFFFFFF;
	public static final int UNLIT = 0xFF560000;
	public static final int LIT_RED = LIT >> 16;
	public static final int UNLIT_RED = UNLIT >> 16;
	
	public static List<BlockTintSource> makeLogicGateTints()
	{
		return IntStream.rangeClosed(0, LogicFunctions.maxIndex()).<BlockTintSource>mapToObj(i -> {
			return switch(i) {
				case LogicFunctions.SET_LATCH -> state -> state.getValue(LatchBlock.POWERED) && !InputState.getInput(state).c ? LIT : UNLIT;
				case LogicFunctions.UNSET_LATCH -> state -> !state.getValue(LatchBlock.POWERED) && !InputState.getInput(state).a ? LIT : UNLIT;
				default -> state -> ColorHandlers.getLogicFunctionTint(i, state);
			};
		}).toList();
	}
	
	public static int getLogicFunctionTint(int tintIndex, BlockState state)
	{
		InputState input = InputState.getInput(state);
		return getLogicFunctionTint(tintIndex, input.a, input.b, input.c);
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
	
	public static List<BlockTintSource> makeAlternatorTintSources()
	{
		return IntStream.rangeClosed(0, 3)
			.<BlockTintSource>mapToObj(tintIndex -> state -> state.getValue(AlternatorBlock.AXLE_ROTATION) == tintIndex ? LIT : UNLIT)
			.toList();
	}
	
	public static List<BlockTintSource> makeRedwirePostBlockTints()
	{
		return List.of(
			state -> NO_TINT,
			state -> ((int)Mth.lerp(state.getValue(AbstractPoweredWirePostBlock.POWER) / 15D, UNLIT_RED, LIT_RED)) << 16
		);
	}
	
	public static List<BlockTintSource> makeRedAlloyWireBlockTints()
	{
		return IntStream.rangeClosed(0, 18)
			.<BlockTintSource>mapToObj(RedAlloyWireTintSource::new)
			.toList();
	}
	
	public static record RedAlloyWireTintSource(int tintIndex) implements BlockTintSource
	{
		@Override
		public int color(BlockState state)
		{
			return tintIndex == 0 // particle
				? UNLIT
				: NO_TINT;
		}

		@Override
		public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos)
		{
			if (tintIndex < 0 || tintIndex > 18) // no tint specified / unused
				return NO_TINT;
			if (tintIndex == 0) // reserved for particle, particle tint is hardcoded to 0
				return UNLIT;
			if (level.getBlockEntity(pos) instanceof PoweredWireBlockEntity wire)
			{
				if (tintIndex < 7) // range is [1,6], indicating a face tint
				{
					int side = tintIndex-1;
					int power = wire.getPower(side);
					double lerpFactor = power/15D;
					return ((int)Mth.lerp(lerpFactor, UNLIT_RED, LIT_RED)) << 16;
				}
				else // range is [7,18], indicating an edge tint
				{
					// average litness from neighbor wires
					int edgeIndex = tintIndex - 7;
					Edge edge = Edge.values()[edgeIndex];
					Direction directionA = edge.sideA;
					BlockPos neighborPosA = pos.relative(directionA);
					BlockEntity neighborTileA = level.getBlockEntity(neighborPosA);
					if (neighborTileA instanceof PoweredWireBlockEntity neighborWireA)
					{
						Direction directionB = edge.sideB;
						BlockPos neighborPosB = pos.relative(directionB);
						BlockEntity neighborTileB = level.getBlockEntity(neighborPosB);
						if (neighborTileB instanceof PoweredWireBlockEntity neighborWireB)
						{
							double powerA = neighborWireA.getPower(directionB);
							double powerB = neighborWireB.getPower(directionA);
							double averagePower = (powerA + powerB)/2D;
							double lerpFactor = averagePower/15D;
							return ((int)Mth.lerp(lerpFactor, UNLIT_RED, LIT_RED)) << 16;
						}
					}
				}
			}
			return NO_TINT;
		}	
	}
}
