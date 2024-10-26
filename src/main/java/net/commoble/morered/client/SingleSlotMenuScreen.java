package net.commoble.morered.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class SingleSlotMenuScreen<MENU extends AbstractContainerMenu> extends AbstractContainerScreen<MENU>
{
	public static final ResourceLocation DISPENSER_GUI = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");
	
	public SingleSlotMenuScreen(MENU screenContainer, Inventory inv, Component titleIn)
	{
		super(screenContainer, inv, titleIn);
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, float partialTicks)
	{
		super.render(graphics, x, y, partialTicks);
		this.renderTooltip(graphics, x, y);
	}
	
	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
	{
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		// window start x/y, texture start x/y, size x/y
		graphics.blit(DISPENSER_GUI, xStart, yStart, 0, 0, this.imageWidth, this.imageHeight);
		// dispenser gui has 3x3 slots
		// hide the outer slots and leave the middle one
		int outerGridStartX = 61;
		int outerGridStartY = 16;
		int squareSize = 18;
		int leftHiderStartX = outerGridStartX - squareSize;
		// left slots
		graphics.blit(DISPENSER_GUI, xStart + outerGridStartX, yStart + outerGridStartY, leftHiderStartX, outerGridStartY, squareSize, squareSize*3);
		// right slots
		graphics.blit(DISPENSER_GUI, xStart + outerGridStartX + squareSize*2, yStart + outerGridStartY, leftHiderStartX, outerGridStartY, squareSize, squareSize*3);
		// top slots
		graphics.blit(DISPENSER_GUI, xStart + outerGridStartX + squareSize, yStart + outerGridStartY, leftHiderStartX, outerGridStartY, squareSize, squareSize);
		// bottom slots
		graphics.blit(DISPENSER_GUI, xStart + outerGridStartX + squareSize, yStart + outerGridStartY + squareSize*2, leftHiderStartX, outerGridStartY, squareSize, squareSize);
	}
}
