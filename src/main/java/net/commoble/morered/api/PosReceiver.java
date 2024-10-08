package net.commoble.morered.api;

import net.minecraft.core.BlockPos;

public record PosReceiver(BlockPos pos, SignalReceiver receiver)
{

}
