package com.github.commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.util.math.Vec3d;

public class SlackInterpolator
{
	public static List<Vec3d> getInterpolatedDifferences(Vec3d vector)
	{
		List<Vec3d> list = new ArrayList<>();
		int points = 17; // 16 segments
		
		double dx = vector.getX();
		double dy = vector.getY();
		double dz = vector.getZ();
		
		for (int point=0; point<points; point++)
		{
			double startLerp = getFractionalLerp(point, points);
			double startYLerp = getYLerp(startLerp, dy);
			list.add(new Vec3d(startLerp*dx, startYLerp*dy, startLerp*dz));
		}
		
		return list;
	}
	
	public static List<Vec3d> getInterpolatedPoints(Vec3d start, Vec3d end)
	{
		return getInterpolatedDifferences(end.subtract(start)).stream()
			.map(diff -> start.add(diff))
			.collect(Collectors.toList());
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
