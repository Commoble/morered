package commoble.morered.plate_blocks;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public enum InputState
{
	FFF(false, false, false),
	FFT(false, false, true),
	FTF(false, true, false),
	FTT(false, true, true),
	TFF(true, false, false),
	TFT(true, false, true),
	TTF(true, true, false),
	TTT(true, true, true);
	
	public final boolean a;
	public final boolean b;
	public final boolean c;
	
	InputState(boolean a, boolean b, boolean c)
	{
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public boolean getSideState(InputSide side)
	{
		switch(side)
		{
			case A:
				return this.a;
			case B:
				return this.b;
			case C:
				return this.c;
			default:
				return false;
		}
	}
	
	public boolean applyLogic(LogicFunction function)
	{
		return function.apply(this.a, this.b, this.c);
	}
	
	public static InputState getState(boolean a, boolean b, boolean c)
	{
		return InputState.values()[boolsToID(a,b,c)];
	}
	
	public static InputState getInput(BlockState state)
	{
		return getState(
			state.hasProperty(InputSide.A.property) ? state.get(InputSide.A.property) : false,
			state.hasProperty(InputSide.B.property) ? state.get(InputSide.B.property) : false,
			state.hasProperty(InputSide.C.property) ? state.get(InputSide.C.property) : false
		);
	}
	
	public static InputState getWorldPowerState(World world, BlockState state, BlockPos pos)
	{
		return getState(
			InputSide.A.isBlockReceivingPower(world, state, pos),
			InputSide.B.isBlockReceivingPower(world, state, pos),
			InputSide.C.isBlockReceivingPower(world, state, pos)
			);
	}
	
	public static BlockState getUpdatedBlockState(World world, BlockState oldBlockState, BlockPos pos)
	{
		InputState oldInputState = InputState.getInput(oldBlockState);
		InputState newInputState = InputState.getWorldPowerState(world, oldBlockState, pos);
		
		if (oldInputState == newInputState)
		{
			return oldBlockState;
		}
		
		BlockState newBlockState = oldBlockState;
		
		for (InputSide side : InputSide.values())
		{
			if (newBlockState.hasProperty(side.property))
			{
				newBlockState = newBlockState.with(side.property, newInputState.getSideState(side));
			}
		}
		
		return newBlockState;
	}
	
	public static int boolsToID(boolean a, boolean b, boolean c)
	{
		return
			(a ? 4 : 0) +
			(b ? 2 : 0) +
			(c ? 1 : 0);
	}
}
