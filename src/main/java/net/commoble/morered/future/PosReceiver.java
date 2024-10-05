package net.commoble.morered.future;

import net.minecraft.core.BlockPos;

public record PosReceiver(BlockPos pos, SignalReceiver receiver)
{

}
