package net.minecraft.item;

import net.minecraft.block.BlockState;

/** Cheap java hack to access protected methods in BlockItem **/
public final class MoreRedBlockItemHelper
{
	public static BlockState getStateForPlacement(BlockItem item, BlockItemUseContext context)
	{
		return item.getStateForPlacement(context);
	}
}