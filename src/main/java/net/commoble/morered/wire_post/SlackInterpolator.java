package net.commoble.morered.wire_post;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.commoble.morered.util.NestedBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;

public class SlackInterpolator
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
			double startYLerp = getYLerp(startLerp, dy);
			list[point] = new Vec3(startLerp * dx, startYLerp * dy, startLerp * dz);
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

	public static double getFractionalLerp(int current, int max)
	{
		return (double) current / (double) max;
	}

	public static double getYLerp(double lerp, double dY)
	{
		return Math.pow(lerp, Math.log(Math.abs(dY) + 3));
	}
	
	/**
	 * Checks if a placed block would intersect any of a given post block's connections.
	 * @param world The world object
	 * @param postPos the position of the post block whose connections we want to check the intersections of
	 * @param placePos The position the block is being placed at
	 * @param placeState The blockstate being placed
	 * @param remoteConnections The remote connections of the post block
	 * @param checkedPostPositions The positions of wire posts that have already been checked.
	 * Any posts in this list that this post is connected to is also connected to this post, and this connection has been
	 * verified to not intersect the placed block, so we don't need to check again.
	 * @return A Vector3d of the intersecting hit, or null if there was no intersecting hit
	 */
	@Nullable
	public static Vec3 doesBlockStateIntersectAnyWireOfPost(BlockGetter world, BlockPos postPos, BlockPos placePos, BlockState placeState, Map<BlockPos, NestedBoundingBox> remoteConnections, Set<BlockPos> checkedPostPositions)
	{
		for (Entry<BlockPos, NestedBoundingBox> entry : remoteConnections.entrySet())
		{
			BlockPos connectedPos = entry.getKey();
			if (!checkedPostPositions.contains(connectedPos))
			{
				Vec3 hit = SlackInterpolator.doesBlockStateIntersectConnection(postPos, connectedPos, placePos, placeState, entry.getValue(), world);
				if (hit != null)
				{
					return hit;
				}
			}
		}
		return null;
	}
	
	@Nullable
	public static Vec3 doesBlockStateIntersectConnection(BlockPos startPos, BlockPos endPos, BlockPos placePos, BlockState placeState, NestedBoundingBox box, BlockGetter world)
	{
		VoxelShape shape = placeState.getCollisionShape(world, placePos);
		for (AABB aabb : shape.toAabbs())
		{
			if (box.intersects(aabb.move(placePos)))
			{
				// if we confirm the AABB intersects, do a raytrace as well
				boolean lastPosIsHigher = startPos.getY() < endPos.getY();
				BlockPos upperPos = lastPosIsHigher ? endPos : startPos;
				BlockPos lowerPos = lastPosIsHigher ? startPos : endPos; 
				return SlackInterpolator.getWireRaytraceHit(lowerPos, upperPos, world);
			}
		}
		return null;
	}

	@Nullable
	public static Vec3 getWireRaytraceHit(BlockPos lower, BlockPos upper, BlockGetter world)
	{
		Vec3 startVec = Vec3.atCenterOf(lower);
		Vec3 endVec = Vec3.atCenterOf(upper);
		Vec3[] points = getInterpolatedPoints(startVec, endVec);
		WireRayTraceSelectionContext selector = new WireRayTraceSelectionContext(lower, upper);
		int pointCount = points.length;
		int rayTraceCount = pointCount-1;
		for (int i=0; i<rayTraceCount; i++)
		{
			WireRayTraceContext context = new WireRayTraceContext(selector, points[i], points[i+1], Block.COLLIDER, Fluid.NONE);
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
	public static BlockHitResult rayTraceBlocks(BlockGetter world, WireRayTraceContext context)
	{
		return doRayTrace(context, (rayTraceContext, pos) ->
		{
			BlockState state = world.getBlockState(pos);
//			IFluidState fluidState = world.getFluidState(pos);
			Vec3 startVec = rayTraceContext.getFrom();
			Vec3 endVec = rayTraceContext.getTo();
			VoxelShape shape = rayTraceContext.getBlockShape(state, world, pos);
			BlockHitResult result = world.clipWithInteractionOverride(startVec, endVec, pos, shape, state);
//			VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, pos);
//			BlockRayTraceResult fluidResult = fluidShape.rayTrace(startVec, endVec, pos);
//			double resultDistance = result == null ? Double.MAX_VALUE : rayTraceContext.getStartVec().squareDistanceTo(result.getHitVec());
//			double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getStartVec().squareDistanceTo(fluidResult.getHitVec());
			return result;
		}, (rayTraceContext) -> {
			Vec3 difference = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
			return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(difference.x, difference.y, difference.z),BlockPos.containing(rayTraceContext.getTo()));
		});
	}

	static <T> T doRayTrace(WireRayTraceContext context, BiFunction<WireRayTraceContext, BlockPos, T> rayTracer, Function<WireRayTraceContext, T> missFactory)
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
