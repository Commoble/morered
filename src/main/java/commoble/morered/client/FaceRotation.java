package commoble.morered.client;

import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3f;

// expanded rotations similar to ModelRotation but with 24 values instead of 16
// rotations represent an "attachment face" and a "connection side"
// representing a thing attached to one of the six faces and pointing in one of the four directions orthagonal to that face
// the "default" state is assumed to be a thing attached to DOWN and pointing NORTH
public class FaceRotation implements IModelTransform
{
	public static final FaceRotation[] FACE_ROTATIONS = Util.make(() ->
	{
		FaceRotation[] result = new FaceRotation[24];
		int[] r0231 = {0, 180, 270, 90};
		int[] r2013 = {180, 0, 90, 270};
		int[] r1302 = {90, 270, 0, 180};
		int[] r3102 = {270, 90, 0, 180};
		int[] r2031 = {180, 0, 270, 90};
		int[] r0213 = {0, 180, 90, 270};
		
		for (int side=0; side<6; side++)
		{
			for (int subSide=0; subSide < 4; subSide++)
			{
				int x = side == 0 ? 0
					: side == 1 ? 180
					: side == 2 ? 270
					: side == 3 ? 90
					: 0;
				int y = side == 0 ? r0231[subSide]
					: side == 1 ? r2013[subSide]
					: side == 4 ? r1302[subSide]
					: side == 5 ? r3102[subSide]
					: 0;
				int z = side == 2 ? r2031[subSide]
					: side == 3 ? r0213[subSide]
					: side == 4 ? 90
					: side == 5 ? 270
					: 0;
				result[side*4 + subSide] = new FaceRotation(x,y,z);
			}
		}
		
		return result;
	});
	
	public static FaceRotation getFaceRotation(int side, int subSide)
	{
		return FACE_ROTATIONS[side*4 + subSide];
	}
	
	private final TransformationMatrix transformation;
	
	FaceRotation(int x, int y, int z)
	{
		Quaternion quaternion = new Quaternion(new Vector3f(0.0F, 0.0F, 1.0F), (-z), true);
		quaternion.mul(new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), (-y), true));
		quaternion.mul(new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), (-x), true));
		this.transformation = new TransformationMatrix((Vector3f)null, quaternion, (Vector3f)null, (Quaternion)null);
	}

	@Override
	public TransformationMatrix getRotation()
	{
		return this.transformation;
	}
}
