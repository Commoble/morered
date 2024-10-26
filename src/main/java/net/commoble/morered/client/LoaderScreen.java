package net.commoble.morered.client;

import net.commoble.morered.transportation.LoaderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LoaderScreen extends SingleSlotMenuScreen<LoaderMenu>
{
	public static final ResourceLocation DROPPER_SLOT = ResourceLocation.withDefaultNamespace("textures/block/dropper_front_vertical.png");

	public LoaderScreen(LoaderMenu screenContainer, Inventory inv, Component titleIn)
	{
		super(screenContainer, inv, titleIn);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
	{
		super.renderBg(graphics, partialTicks, mouseX, mouseY);
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		graphics.blit(DROPPER_SLOT, xStart + 80, yStart + 35, 0, 0, 16, 16, 16, 16);
	}
}
