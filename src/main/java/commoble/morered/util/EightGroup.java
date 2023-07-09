package commoble.morered.util;

import com.mojang.math.OctahedralGroup;

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
}
