package net.commoble.morered.future;

import net.minecraft.world.level.block.state.BlockState;

public record StateWirer(BlockState state, Wirer wirer)
{

}
