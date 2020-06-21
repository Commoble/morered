package com.github.commoble.morered.wire_post;

import net.minecraft.util.math.Vec3d;

public class SlackInterpolator
{
	public static Vec3d[] getInterpolatedDifferences(Vec3d vector)
	{
		int points = 17; // 16 segments
		Vec3d[] list = new Vec3d[points];
		
		double dx = vector.getX();
		double dy = vector.getY();
		double dz = vector.getZ();
		
		for (int point=0; point<points; point++)
		{
			double startLerp = getFractionalLerp(point, points-1);
			double startYLerp = getYLerp(startLerp, dy);
			list[point] = new Vec3d(startLerp*dx, startYLerp*dy, startLerp*dz);
		}
		
		return list;
	}
	
	public static Vec3d[] getInterpolatedPoints(Vec3d start, Vec3d end)
	{
		Vec3d diff = end.subtract(start);
		Vec3d[] diffs = getInterpolatedDifferences(diff);
		Vec3d[] points = new Vec3d[diffs.length];
		for (int i=0; i<points.length; i++)
		{
			points[i] = start.add(diffs[i]);
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
}
