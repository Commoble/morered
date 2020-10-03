package commoble.morered.util;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import net.minecraft.util.math.AxisAlignedBB;

public class NestedBoundingBox
{
	private final @Nonnull List<NestedBoundingBox> subBoxes;	// immutable list of all sub-boxes nested in this box
	private final @Nonnull AxisAlignedBB superBox;	// union of all AABBs belonging to sub-boxes
	
	private static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(
		Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
		Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
	
	public NestedBoundingBox(@Nonnull NestedBoundingBox boxA, @Nonnull NestedBoundingBox boxB)
	{
		this.subBoxes = Collections.unmodifiableList(Lists.newArrayList(boxA, boxB));
		this.superBox = boxA.superBox.union(boxB.superBox);
//		BlockPos[] blockPosArray = new BlockPos[5];
//		ArrayList<AxisAlignedBB> list = Arrays.stream(blockPosArray).map(AxisAlignedBB::new).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public NestedBoundingBox(@Nonnull AxisAlignedBB box)
	{
		this.subBoxes = Collections.emptyList();
		this.superBox = box;
	}
	
	public NestedBoundingBox combine(@Nonnull NestedBoundingBox other)
	{
		return new NestedBoundingBox(this, other);
	}
	
	public static NestedBoundingBox fromAABBs(AxisAlignedBB ...boxes)
	{
		return fromAABBs(0, boxes.length-1, boxes);
	}
	
	// get a NestedBoundingBox from the aabbs in the list with indices in the range [fromIndex, toIndex] inclusive
	private static NestedBoundingBox fromAABBs(int fromIndex, int toIndex, AxisAlignedBB ...boxes)
	{
		int size = toIndex - fromIndex + 1;
		if (size <= 0)
		{
			return new NestedBoundingBox(EMPTY_AABB);
		}
		else if (size == 1)
		{
			return new NestedBoundingBox(boxes[toIndex]);
		}
		else
		{
			int partition = (fromIndex + toIndex) / 2;
			return new NestedBoundingBox(fromAABBs(fromIndex, partition, boxes), fromAABBs(partition+1, toIndex, boxes));
		}
		
	}
	
	public boolean intersects(@Nonnull AxisAlignedBB target)
	{
		return new IntersectionPredicate(target).test(this);
	}
	
	private static class IntersectionPredicate implements Predicate<NestedBoundingBox>
	{
		private final AxisAlignedBB target;
		
		public IntersectionPredicate(AxisAlignedBB target)
		{
			this.target = target;
		}

		@Override
		public boolean test(NestedBoundingBox box)
		{
			if (!box.superBox.intersects(this.target))
			{
				return false;
			}
			else if (box.subBoxes.isEmpty())
			{	// if the nested box's AABB intersects the target AABB and there are no sub-boxes to check, the target box intersects

//				MoreRed.CHANNEL.send(PacketDistributor.ALL.noArg(),
//					new WireBreakPacket(new Vec3d(box.superBox.minX, box.superBox.minY, box.superBox.minZ), new Vec3d(box.superBox.maxX, box.superBox.maxY, box.superBox.maxZ)));
				return true;
			}
			else
			{
				return box.subBoxes.parallelStream().anyMatch(this);
			}
		}
		
	}
}
