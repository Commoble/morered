package net.commoble.morered.client;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MechanicalItemRenderState
{
	public final ItemStackRenderState itemState = new ItemStackRenderState();
	public float radians = 0F;
	
	public void update(ItemModelResolver resolver, Level level, ItemStack stack, float radians, BlockPos blockPos)
	{
		resolver.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, level, null, (int)(blockPos.asLong()));
	}
}
