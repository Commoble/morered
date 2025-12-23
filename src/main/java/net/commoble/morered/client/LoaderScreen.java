package net.commoble.morered.client;

import net.commoble.morered.transportation.LoaderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class LoaderScreen extends SingleSlotMenuScreen<LoaderMenu>
{
	public static final Identifier DROPPER_SLOT = Identifier.withDefaultNamespace("textures/block/dropper_front_vertical.png");

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
		graphics.blit(RenderPipelines.GUI_TEXTURED, DROPPER_SLOT, xStart + 80, yStart + 35, 0, 0, 16, 16, 16, 16);
	}
}
