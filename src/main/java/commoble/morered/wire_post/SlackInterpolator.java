package commoble.morered.wire_post;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import commoble.morered.util.NestedBoundingBox;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;

public class SlackInterpolator
{
	public static Vector3d[] getInterpolatedDifferences(Vector3d vector)
	{
		int points = 17; // 16 segments
		Vector3d[] list = new Vector3d[points];

		double dx = vector.x();
		double dy = vector.y();
		double dz = vector.z();

		for (int point = 0; point < points; point++)
		{
			double startLerp = getFractionalLerp(point, points - 1);
			double startYLerp = getYLerp(startLerp, dy);
			list[point] = new Vector3d(startLerp * dx, startYLerp * dy, startLerp * dz);
		}

		return list;
	}

	public static Vector3d[] getInterpolatedPoints(Vector3d lower, Vector3d upper)
	{
		Vector3d diff = upper.subtract(lower);
		Vector3d[] diffs = getInterpolatedDifferences(diff);
		Vector3d[] points = new Vector3d[diffs.length];
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
	public static Vector3d doesBlockStateIntersectAnyWireOfPost(IBlockReader world, BlockPos postPos, BlockPos placePos, BlockState placeState, Map<BlockPos, NestedBoundingBox> remoteConnections, Set<BlockPos> checkedPostPositions)
	{
		for (Entry<BlockPos, NestedBoundingBox> entry : remoteConnections.entrySet())
		{
			BlockPos connectedPos = entry.getKey();
			if (!checkedPostPositions.contains(connectedPos))
			{
				Vector3d hit = SlackInterpolator.doesBlockStateIntersectConnection(postPos, connectedPos, placePos, placeState, entry.getValue(), world);
				if (hit != null)
				{
					return hit;
				}
			}
		}
		return null;
	}
	
	@Nullable
	public static Vector3d doesBlockStateIntersectConnection(BlockPos startPos, BlockPos endPos, BlockPos placePos, BlockState placeState, NestedBoundingBox box, IBlockReader world)
	{
		VoxelShape shape = placeState.getCollisionShape(world, placePos);
		for (AxisAlignedBB aabb : shape.toAabbs())
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
	public static Vector3d getWireRaytraceHit(BlockPos lower, BlockPos upper, IBlockReader world)
	{
		Vector3d startVec = WirePostTileEntity.getConnectionVector(lower);
		Vector3d endVec = WirePostTileEntity.getConnectionVector(upper);
		Vector3d[] points = getInterpolatedPoints(startVec, endVec);
		WireRayTraceSelectionContext selector = new WireRayTraceSelectionContext(lower, upper);
		int pointCount = points.length;
		int rayTraceCount = pointCount-1;
		for (int i=0; i<rayTraceCount; i++)
		{
			WireRayTraceContext context = new WireRayTraceContext(selector, points[i], points[i+1], BlockMode.COLLIDER, FluidMode.NONE);
			BlockRayTraceResult result = rayTraceBlocks(world, context);
			if (result.getType() != RayTraceResult.Type.MISS)
			{
				return result.getLocation();
			}
		}
		
		return null; // didn't hit
		
	}

	// vanilla raytracer requires a non-null entity when the context is constructed
	// we don't need an entity though
	public static BlockRayTraceResult rayTraceBlocks(IBlockReader world, WireRayTraceContext context)
	{
		return doRayTrace(context, (rayTraceContext, pos) ->
		{
			BlockState state = world.getBlockState(pos);
//			IFluidState fluidState = world.getFluidState(pos);
			Vector3d startVec = rayTraceContext.getStartVec();
			Vector3d endVec = rayTraceContext.getEndVec();
			VoxelShape shape = rayTraceContext.getBlockShape(state, world, pos);
			BlockRayTraceResult result = world.clipWithInteractionOverride(startVec, endVec, pos, shape, state);
//			VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, pos);
//			BlockRayTraceResult fluidResult = fluidShape.rayTrace(startVec, endVec, pos);
//			double resultDistance = result == null ? Double.MAX_VALUE : rayTraceContext.getStartVec().squareDistanceTo(result.getHitVec());
//			double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getStartVec().squareDistanceTo(fluidResult.getHitVec());
			return result;
		}, (rayTraceContext) -> {
			Vector3d difference = rayTraceContext.getStartVec().subtract(rayTraceContext.getEndVec());
			return BlockRayTraceResult.miss(rayTraceContext.getEndVec(), Direction.getNearest(difference.x, difference.y, difference.z), new BlockPos(rayTraceContext.getEndVec()));
		});
	}

	static <T> T doRayTrace(WireRayTraceContext context, BiFunction<WireRayTraceContext, BlockPos, T> rayTracer, Function<WireRayTraceContext, T> missFactory)
	{
		Vector3d start = context.getStartVec();
		Vector3d end = context.getEndVec();
		if (start.equals(end))
		{
			return missFactory.apply(context);
		}
		else
		{
			double endX = MathHelper.lerp(-1.0E-7D, end.x, start.x);
			double endY = MathHelper.lerp(-1.0E-7D, end.y, start.y);
			double endZ = MathHelper.lerp(-1.0E-7D, end.z, start.z);
			double startX = MathHelper.lerp(-1.0E-7D, start.x, end.x);
			double startY = MathHelper.lerp(-1.0E-7D, start.y, end.y);
			double startZ = MathHelper.lerp(-1.0E-7D, start.z, end.z);
			int startXInt = MathHelper.floor(startX);
			int startYInt = MathHelper.floor(startY);
			int startZInt = MathHelper.floor(startZ);
			BlockPos.Mutable mutaPos = new BlockPos.Mutable(startXInt, startYInt, startZInt);
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
				int xSign = MathHelper.sign(dx);
				int ySign = MathHelper.sign(dy);
				int zSign = MathHelper.sign(dz);
				double reciprocalX = xSign == 0 ? Double.MAX_VALUE : xSign / dx;
				double reciprocalY = ySign == 0 ? Double.MAX_VALUE : ySign / dy;
				double reciprocalZ = zSign == 0 ? Double.MAX_VALUE : zSign / dz;
				double calcX = reciprocalX * (xSign > 0 ? 1.0D - MathHelper.frac(startX) : MathHelper.frac(startX));
				double calcY = reciprocalY * (ySign > 0 ? 1.0D - MathHelper.frac(startY) : MathHelper.frac(startY));
				double calcZ = reciprocalZ * (zSign > 0 ? 1.0D - MathHelper.frac(startZ) : MathHelper.frac(startZ));

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
