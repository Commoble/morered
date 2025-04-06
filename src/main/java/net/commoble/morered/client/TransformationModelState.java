package net.commoble.morered.client;

import com.mojang.math.Transformation;

import net.minecraft.client.resources.model.ModelState;

public record TransformationModelState(Transformation transform) implements ModelState
{

}
