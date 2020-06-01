package com.github.commoble.morered.client;

import com.github.commoble.morered.BlockRegistrar;
import com.github.commoble.morered.gatecrafting_plinth.GatecraftingContainer;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class GatecraftingScreen extends ContainerScreen<GatecraftingContainer>
{
	public static final ResourceLocation TRADING_SCREEN = new ResourceLocation("minecraft:textures/gui/container/villager2.png");
	public static final ResourceLocation CRAFTING_SCREEN = new ResourceLocation("minecraft:textures/gui/container/crafting_table.png");
	
	private final String name;

	public GatecraftingScreen(GatecraftingContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
	{
		super(screenContainer, inv, titleIn);
		this.xSize = 276;
		this.ySize = 166;
		this.name = new TranslationTextComponent(BlockRegistrar.GATECRAFTING_PLINTH.get().getTranslationKey()).getFormattedText();
	}

	@Override
	public void render(int x, int y, float partialTicks)
	{
		this.renderBackground();
		super.render(x, y, partialTicks);
		this.renderHoveredToolTip(x, y);
	}


	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		String playerName = this.playerInventory.getName().getFormattedText();
		this.font.drawString(this.name, this.xSize/2 - this.font.getStringWidth(this.name)/2, 6, 4210752);	// y-value and color from dispenser, etc
		this.font.drawString(playerName, 107, this.ySize-96+2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
	{
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		int xStart = (this.width - this.xSize) / 2;
		int yStart = (this.height - this.ySize) / 2;
		
		// use the trading window as the main background
		this.minecraft.getTextureManager().bindTexture(TRADING_SCREEN);
		blit(xStart,  yStart, 0, 0, this.xSize, this.ySize, 512, 256);
		
		// draw the 3x3 crafting table slots over it
		int craftMatrixScreenX = xStart + 125;
		int craftMatrixScreenY = yStart + 16;
		int craftMatrixU = 29;
		int craftMatrixV = 16;
		int craftMatrixWidth = 116;
		int craftMatrixHeight = 54;
		this.minecraft.getTextureManager().bindTexture(CRAFTING_SCREEN);
		this.blit(craftMatrixScreenX,  craftMatrixScreenY, craftMatrixU, craftMatrixV, craftMatrixWidth, craftMatrixHeight);
	}

}
