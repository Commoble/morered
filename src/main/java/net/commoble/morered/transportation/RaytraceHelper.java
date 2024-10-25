package net.commoble.morered.transportation;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.commoble.morered.util.NestedBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RaytraceHelper
{
	public static Vec3[] getInterpolatedDifferences(Vec3 vector)
	{
		int points = 17; // 16 segments
		Vec3[] list = new Vec3[points];

		double dx = vector.x();
		double dy = vector.y();
		double dz = vector.z();

		for (int point = 0; point < points; point++)
		{
			double startLerp = getFractionalLerp(point, points - 1);
			list[point] = new Vec3(startLerp * dx, startLerp * dy, startLerp * dz);
		}

		return list;
	}

	public static Vec3[] getInterpolatedPoints(Vec3 lower, Vec3 upper)
	{
		Vec3 diff = upper.subtract(lower);
		Vec3[] diffs = getInterpolatedDifferences(diff);
		Vec3[] points = new Vec3[diffs.length];
		for (int i = 0; i < points.length; i++)
		{
			points[i] = lower.add(diffs[i]);
		}
		return points;
	}

	
	/**
	 * Checks if a placed block would intersect any of the connections of the tube block at the given position
	 * @param tubePos the position of the tube
	 * @param placePos The position the block is being placed at
	 * @param raytraceWorld the world to do raytracing in -- must have the block we are trying to hit with the raytrace
	 * @param placeState The blockstate being placed
	 * @param checkedTubePositions The positions of tubes that have already been checked.
	 * Any tubes in this list that this tube is connected to is also connected to this tube, and this connection has been
	 * verified to not intersect the placed block, so we don't need to check again.
	 * @param connections the remote connections of the given tube
	 * @return A Vec3 of the intersecting hit, or null if there was no intersecting hit
	 */
	@Nullable
	public static Vec3 doesBlockStateIntersectTubeConnections(BlockPos tubePos, BlockPos placePos, BlockGetter raytraceWorld, @Nonnull BlockState placeState, Set<BlockPos> checkedTubePositions, Map<Direction, RemoteConnection> connections)
	{
		for (Map.Entry<Direction, RemoteConnection> entry : connections.entrySet())
		{
			RemoteConnection connection = entry.getValue();
			BlockPos pos = connection.toPos;
			if (!checkedTubePositions.contains(pos))
			{
				Direction fromSide = entry.getKey();
				Direction toSide = connection.toSide;
				Vec3 hit = doesBlockStateIntersectConnection(tubePos, fromSide, pos, toSide, placePos, placeState, connection.getBox(), raytraceWorld);
				if (hit != null)
				{
					return hit;
				}
			}
		}
		return null;
	}
	
	@Nullable
	public static Vec3 doesBlockStateIntersectConnection(BlockPos startPos, Direction startSide, BlockPos endPos, Direction endSide, BlockPos placePos, @Nonnull BlockState placeState, NestedBoundingBox box, BlockGetter world)
	{
		VoxelShape shape = placeState.getCollisionShape(world, placePos);
		for (AABB aabb : shape.toAabbs())
		{
			if (box.intersects(aabb.move(placePos)))
			{
				// if we confirm the AABB intersects, do a raytrace as well
				Vec3 startVec = RaytraceHelper.getTubeSideCenter(startPos, startSide);
				Vec3 endVec = RaytraceHelper.getTubeSideCenter(endPos, endSide);
				return RaytraceHelper.getTubeRaytraceHit(startVec, endVec, world);
			}
		}
		return null;
	}

	public static double getFractionalLerp(int current, int max)
	{
		return (double) current / (double) max;
	}
	
	/** Returns the vector representing the center of the side of a tube block **/
	public static Vec3 getTubeSideCenter(BlockPos pos, Direction side)
	{
		Vec3 center = Vec3.atCenterOf(pos);
		double offsetFromCenter = 4D/16D;
		double xOff = side.getStepX() * offsetFromCenter;
		double yOff = side.getStepY() * offsetFromCenter;
		double zOff = side.getStepZ() * offsetFromCenter;
		return center.add(xOff, yOff, zOff);
	}

	@Nullable
	public static Vec3 getTubeRaytraceHit(Vec3 startVec, Vec3 endVec, BlockGetter world)
	{
		Vec3[] points = getInterpolatedPoints(startVec, endVec);
		int pointCount = points.length;
		int rayTraceCount = pointCount-1;
		for (int i=0; i<rayTraceCount; i++)
		{
			ClipContext context = new ClipContext(points[i], points[i+1], ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
			BlockHitResult result = rayTraceBlocks(world, context);
			if (result.getType() != HitResult.Type.MISS)
			{
				return result.getLocation();
			}
		}
		
		return null; // didn't hit
		
	}

	// vanilla raytracer requires a non-null entity when the context is constructed
	// we don't need an entity though
	public static BlockHitResult rayTraceBlocks(BlockGetter world, ClipContext context)
	{
		return doRayTrace(context, (rayTraceContext, pos) ->
		{
			BlockState state = world.getBlockState(pos);
			Vec3 startVec = rayTraceContext.getFrom();
			Vec3 endVec = rayTraceContext.getTo();
			VoxelShape shape = rayTraceContext.getBlockShape(state, world, pos);
			BlockHitResult result = world.clipWithInteractionOverride(startVec, endVec, pos, shape, state);
			return result;
		}, (rayTraceContext) -> {
			Vec3 difference = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
			return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(difference.x, difference.y, difference.z), BlockPos.containing(rayTraceContext.getTo()));
		});
	}

	static <T> T doRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> rayTracer, Function<ClipContext, T> missFactory)
	{
		Vec3 start = context.getFrom();
		Vec3 end = context.getTo();
		if (start.equals(end))
		{
			return missFactory.apply(context);
		}
		else
		{
			double endX = Mth.lerp(-1.0E-7D, end.x, start.x);
			double endY = Mth.lerp(-1.0E-7D, end.y, start.y);
			double endZ = Mth.lerp(-1.0E-7D, end.z, start.z);
			double startX = Mth.lerp(-1.0E-7D, start.x, end.x);
			double startY = Mth.lerp(-1.0E-7D, start.y, end.y);
			double startZ = Mth.lerp(-1.0E-7D, start.z, end.z);
			int startXInt = Mth.floor(startX);
			int startYInt = Mth.floor(startY);
			int startZInt = Mth.floor(startZ);
			BlockPos.MutableBlockPos mutaPos = new BlockPos.MutableBlockPos(startXInt, startYInt, startZInt);
			T result = rayTracer.apply(context, mutaPos);
			if (result != null)
			{
				return result;
			}
			else
			{
				double dx = endX - startX;
				double dy = endY - startY;
				double dz = endZ - startZ;
				int xSign = Mth.sign(dx);
				int ySign = Mth.sign(dy);
				int zSign = Mth.sign(dz);
				double reciprocalX = xSign == 0 ? Double.MAX_VALUE : xSign / dx;
				double reciprocalY = ySign == 0 ? Double.MAX_VALUE : ySign / dy;
				double reciprocalZ = zSign == 0 ? Double.MAX_VALUE : zSign / dz;
				double calcX = reciprocalX * (xSign > 0 ? 1.0D - Mth.frac(startX) : Mth.frac(startX));
				double calcY = reciprocalY * (ySign > 0 ? 1.0D - Mth.frac(startY) : Mth.frac(startY));
				double calcZ = reciprocalZ * (zSign > 0 ? 1.0D - Mth.frac(startZ) : Mth.frac(startZ));

				while (calcX <= 1.0D || calcY <= 1.0D || calcZ <= 1.0D)
				{
					if (calcX < calcY)
					{
						if (calcX < calcZ)
						{
							startXInt += xSign;
							calcX += reciprocalX;
						}
						else
						{
							startZInt += zSign;
							calcZ += reciprocalZ;
						}
					}
					else if (calcY < calcZ)
					{
						startYInt += ySign;
						calcY += reciprocalY;
					}
					else
					{
						startZInt += zSign;
						calcZ += reciprocalZ;
					}

					T fallbackResult = rayTracer.apply(context, mutaPos.set(startXInt, startYInt, startZInt));
					if (fallbackResult != null)
					{
						return fallbackResult;
					}
				}

				return missFactory.apply(context);
			}
		}
	}
}
