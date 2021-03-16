package commoble.morered.plate_blocks;

import commoble.morered.util.BlockStateUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public enum InputSide
{
	A(PlateBlockStateProperties.INPUT_A, 1),
	B(PlateBlockStateProperties.INPUT_B, 2),
	C(PlateBlockStateProperties.INPUT_C, 3);
	
	public final BooleanProperty property;
	public final int rotationsFromOutput;
	
	InputSide(BooleanProperty property, int rotationsFromOutput)
	{
		this.property = property;
		this.rotationsFromOutput = rotationsFromOutput;
	}
	
	/**
	 * Returns true if a given block with the relevant blockstate properties is receiving power from the
	 * direction associated with this input side. Returns false if not, or if the block lacks the
	 * relevant properties.
	 * @param world
	 * @param state
	 * @param pos
	 * @return
	 */
	public boolean isBlockReceivingPower(World world, BlockState state, BlockPos pos)
	{
		// return early if the state doesn't care about this side or we're checking an invalid state
		if(!state.hasProperty(this.property) || !state.hasProperty(PlateBlockStateProperties.ATTACHMENT_DIRECTION) || !state.hasProperty(PlateBlockStateProperties.ROTATION))
		{
			return false;
		}
		
		Direction attachmentDirection = state.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		int baseRotation = state.getValue(PlateBlockStateProperties.ROTATION);
		
		Direction inputDirection = BlockStateUtil.getInputDirection(attachmentDirection, baseRotation, this.rotationsFromOutput);
		

		BlockPos inputPos = pos.relative(inputDirection);

		int power = world.getSignal(inputPos, inputDirection);
		if (power > 0)
		{
			return true;
		}
		else
		{
			BlockState inputState = world.getBlockState(inputPos);
			return (inputState.getBlock() == Blocks.REDSTONE_WIRE && inputState.getValue(RedstoneWireBlock.POWER) > 0);
		}
	}
}
