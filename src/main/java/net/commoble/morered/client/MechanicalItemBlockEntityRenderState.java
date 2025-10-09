package net.commoble.morered.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MechanicalItemBlockEntityRenderState extends BlockEntityRenderState
{
	public final ItemStackRenderState itemState = new ItemStackRenderState();
	public float radians = 0F;
	
	public void update(ItemModelResolver resolver, Level level, ItemStack stack, float radians)
	{
		resolver.updateForTopItem(this.itemState, stack, ItemDisplayContext.NONE, level, null, (int)(this.blockPos.asLong()));
		this.radians = radians;
	}
	
	public void clear()
	{
		this.itemState.clear();
		this.radians = 0F;
	}
}
