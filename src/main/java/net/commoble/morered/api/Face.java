package net.commoble.morered.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record Face(BlockPos pos, Direction attachmentSide)
{

}
