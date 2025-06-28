package net.commoble.morered.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class StandardSizeContainerScreenFactory<ContainerType extends AbstractContainerMenu> implements MenuScreens.ScreenConstructor<ContainerType, AbstractContainerScreen<ContainerType>>
{
	// location of GUI texture
	private final ResourceLocation texture;
	private final String windowTitleTranslationKey;

	private StandardSizeContainerScreenFactory(ResourceLocation texture, String windowTitleTranslationKey)
	{
		this.texture = texture;
		this.windowTitleTranslationKey = windowTitleTranslationKey;
	}
	
	public static <ContainerType extends AbstractContainerMenu> StandardSizeContainerScreenFactory<ContainerType> of(ResourceLocation texture, String translationKey)
	{
		return new StandardSizeContainerScreenFactory<>(texture, translationKey);
	}

	@Override
	public AbstractContainerScreen<ContainerType> create(ContainerType container, Inventory inventory, Component name)
	{
		return new StandardSizeContainerScreen<ContainerType>(container, inventory, name, this.texture, this.windowTitleTranslationKey);
	}
	
	static class StandardSizeContainerScreen<ContainerType extends AbstractContainerMenu> extends AbstractContainerScreen<ContainerType>
	{
		private final ResourceLocation texture;
		
		public StandardSizeContainerScreen(ContainerType screenContainer, Inventory inv, Component titleIn, ResourceLocation texture, String windowTitleTranslationKey)
		{
			super(screenContainer, inv, titleIn);
			this.imageWidth = 176;
			this.imageHeight = 166;
			this.texture = texture;
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
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, xStart,  yStart, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
		}
	}
}
