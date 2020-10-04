package commoble.morered.plate_blocks;

import commoble.morered.util.BlockStateUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

/**
 * The three input booleanproperties here are the inputs 90, 180, and 270 degrees clockwise from the output
 * e.g.
 * 
 		OUTPUT
		/-----\
		|     |
	C	|     | A
		|     |
		\-----/
		   B
 * For consistency, logic plates with fewer than three inputs still use the input property for the appropriate side
 * (a gate with one input opposite from the output should use the input B property)
 */
public class PlateBlockStateProperties
{
	public static final DirectionProperty ATTACHMENT_DIRECTION = BlockStateProperties.FACING;
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0,3);
	
	/** The first input clockwise from the output **/
	public static final BooleanProperty INPUT_A = BooleanProperty.create("input_a");
	/** The second input clockwise from the output **/
	public static final BooleanProperty INPUT_B = BooleanProperty.create("input_b");
	/** The third input clockwise from the output **/
	public static final BooleanProperty INPUT_C = BooleanProperty.create("input_c");
	
	public static final Material PLATE_MATERIAL = new Material(MaterialColor.STONE, false, true, true, true, false, false, PushReaction.NORMAL);

	
	public static Direction getOutputDirection(BlockState state)
	{
		if (!state.hasProperty(ATTACHMENT_DIRECTION) || !state.hasProperty(ROTATION))
		{
			return Direction.DOWN;
		}
		
		Direction attachmentDirection = state.get(ATTACHMENT_DIRECTION);
		int rotationIndex = state.get(ROTATION);
		
		return BlockStateUtil.getOutputDirection(attachmentDirection, rotationIndex);
	}
	
	public static BlockState getStateForPlacedGatePlate(BlockState state, BlockItemUseContext context)
	{
		// how do we want to orient the block when we place it?
		// attachment face should be the face that was clicked
		// what about the rotation?
		
		// option A: output faces away from player, inverted when sneaking
		// empirical observations: this is super annoying to get the player in the right standing position,
		// especially when placing the thing on a wall
		
		// option B: the orientation depends on which part of the face was clicked, not the player's facing
		// this gives more control to the player but might be confusing
		// we may want to render a preview of the placement somehow

		BlockPos placePos = context.getPos();
		Direction faceOfAdjacentBlock = context.getFace();
		Direction directionTowardAdjacentBlock = faceOfAdjacentBlock.getOpposite();
		Vector3d relativeHitVec = context.getHitVec().subtract(Vector3d.copy(placePos));
		return getStateForPlacedGatePlate(state, placePos, directionTowardAdjacentBlock, relativeHitVec); 
	}
	
	public static BlockState getStateForPlacedGatePlate(BlockState state, BlockPos placePos, Direction directionTowardAdjacentBlock, Vector3d relativeHitVec)
	{
		Direction outputDirection = BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec, directionTowardAdjacentBlock);
		int rotationIndex = BlockStateUtil.getRotationIndexForDirection(directionTowardAdjacentBlock, outputDirection);
		
		if (state.hasProperty(ATTACHMENT_DIRECTION) && state.hasProperty(ROTATION))
		{
			return state.with(ATTACHMENT_DIRECTION, directionTowardAdjacentBlock).with(ROTATION, rotationIndex);
		}
		else
		{
			return state;
		}
	}
}
