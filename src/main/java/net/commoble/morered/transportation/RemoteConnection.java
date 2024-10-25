package net.commoble.morered.transportation;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.util.NestedBoundingBox;
import net.commoble.morered.util.PosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RemoteConnection
{	
	public final Direction toSide;
	public final BlockPos toPos;
	/** Every connection is stored inside both tubes, but only the primary connection will be rendered **/
	public final boolean isPrimary;
	private final BlockPos fromPos;
	private NestedBoundingBox box;
	
	public RemoteConnection(Direction fromSide, Direction toSide, BlockPos fromPos, BlockPos toPos, boolean isPrimary)
	{
		this.toSide = toSide;
		this.toPos = toPos;
		this.isPrimary = isPrimary;
		this.fromPos = fromPos;
	}
	
	public NestedBoundingBox getBox()
	{
		if (this.box == null)
			this.box = getNestedBoundingBoxForConnectedPos(this.fromPos, this.toPos);
		return this.box;
	}
	
	public Storage toStorage(BlockPos tubePos)
	{
		return new Storage(this.toSide, this.toPos.subtract(tubePos), this.isPrimary);
	}
	
	public static RemoteConnection fromStorage(Storage storage, Direction fromSide, BlockPos fromPos)
	{
		return new RemoteConnection(fromSide, storage.toSide, fromPos, storage.toPos.offset(fromPos), storage.isPrimary);
	}
	
	private static NestedBoundingBox getNestedBoundingBoxForConnectedPos(BlockPos from, BlockPos to)
	{
		Vec3 thisVec = Vec3.atCenterOf(from);
		Vec3 otherVec = Vec3.atCenterOf(to);
		boolean otherHigher = otherVec.y > thisVec.y;
		Vec3 higherVec = otherHigher ? otherVec : thisVec;
		Vec3 lowerVec = otherHigher ? thisVec : otherVec;
		Vec3[] points = RaytraceHelper.getInterpolatedPoints(lowerVec, higherVec);
		int segmentCount = points.length - 1;
		AABB[] boxes = new AABB[segmentCount];
		for (int i=0; i<segmentCount; i++)
		{
			boxes[i] = new AABB(points[i], points[i+1]);
		}
		return NestedBoundingBox.fromAABBs(boxes);
	}
	
	public static class Storage
	{
		public final Direction toSide;
		public final BlockPos toPos;
		public final boolean isPrimary;
		
		public Storage(Direction toSide, BlockPos toPos, boolean isPrimary)
		{
			this.toSide = toSide;
			this.toPos = toPos;
			this.isPrimary = isPrimary;
		}
		
		/**
		 * Reads from nbt, denormalizing position and side
		 * @param nbt CompoundTag being read
		 * @param group OctahedralGroup of the tube being loaded, the tube's rotation from e.g. a structure piece.
		 * Must rotate directions and blockspos to "denormalize" them.
		 * @return Storage
		 */
		public static Storage fromNBT(CompoundTag nbt, OctahedralGroup group)
		{
			Direction toSide = group.rotate(Direction.byName(nbt.getString("toSide")));
			BlockPos toPos = PosHelper.transform(NbtUtils.readBlockPos(nbt, "toPos").orElse(BlockPos.ZERO), group);
			boolean isPrimary = nbt.getBoolean("isPrimary");
			return new Storage(toSide, toPos, isPrimary);
		}
		
		/**
		 * Normalizes position and side and writes to nbt
		 * @param group OctahedralGroup representing the orientation of the tube.
		 * The inverse of this will apply to position and side.
		 * @return
		 */
		public CompoundTag toNBT(OctahedralGroup group)
		{
			OctahedralGroup normalizer = group.inverse();
			CompoundTag nbt = new CompoundTag();
			nbt.putString("toSide", normalizer.rotate(this.toSide).getName());
			nbt.put("toPos", NbtUtils.writeBlockPos(PosHelper.transform(this.toPos, normalizer)));
			nbt.putBoolean("isPrimary", this.isPrimary);
			return nbt;
		}
	}
}
