package net.commoble.morered.client;

import com.mojang.math.Transformation;

import net.minecraft.client.resources.model.ModelState;

public record TransformationModelState(Transformation transformation) implements ModelState
{
	@Override
	public Transformation transformation()
	{
		return this.transformation;
	}
}
