package net.commoble.morered.util;

import com.mojang.math.OctahedralGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The eight-group is the set of eight transformations that can be applied to a block via combinations of rotate() and mirror().
 * Lots of things can apply rotation, mostly jigsaw structures.
 * Mirror isn't widely used, structure blocks support it but jigsaw structures don't currently mirror structure pieces.
 * We use the eight-group as a blockstate property so blockentities that store positional data can respect structure piece transformations.
 */
public class EightGroup
{
	private static final OctahedralGroup[] STRUCTURE_TRANSFORMS = {
		OctahedralGroup.IDENTITY,
		OctahedralGroup.ROT_180_FACE_XZ,
		OctahedralGroup.ROT_90_Y_NEG,
		OctahedralGroup.ROT_90_Y_POS,
		OctahedralGroup.INVERT_X,
		OctahedralGroup.INVERT_Z,
		OctahedralGroup.SWAP_XZ,
		OctahedralGroup.SWAP_NEG_XZ
	};
	public static final EnumProperty<OctahedralGroup> TRANSFORM = EnumProperty.create("transform", OctahedralGroup.class, STRUCTURE_TRANSFORMS);
	
	public static BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(TRANSFORM, rotation.rotation().compose(state.getValue(TRANSFORM)));
	}
	
	public static BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.setValue(TRANSFORM, mirror.rotation().compose(state.getValue(TRANSFORM)));
	}
	
	/**
	 * 
	 * @param pos BlockPos to rotate about the origin
	 * @param group OctahedralGroup to rotate the pos with
	 * @return BlockPos rotated by the group
	 */
	public static BlockPos transform(BlockPos pos, OctahedralGroup group)
	{
		// averts most of the logic in most cases
		if (group == OctahedralGroup.IDENTITY)
		{
			return pos;
		}
		
		BlockPos newPos = new BlockPos(0,0,0);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		if (x != 0)
		{
			Direction oldDirX = x > 0 ? Direction.EAST : Direction.WEST;
			Direction newDirX = group.rotate(oldDirX);
			newPos = newPos.relative(newDirX, Math.abs(x));
		}
		if (y != 0)
		{
			Direction oldDirY = y > 0 ? Direction.UP : Direction.DOWN;
			Direction newDirY = group.rotate(oldDirY);
			newPos = newPos.relative(newDirY, Math.abs(y));
		}
		if (z != 0)
		{
			Direction oldDirZ = z > 0 ? Direction.SOUTH : Direction.NORTH;
			Direction newDirZ = group.rotate(oldDirZ);
			newPos = newPos.relative(newDirZ, Math.abs(z));
		}
		return newPos;
	}
}
