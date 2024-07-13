package commoble.morered.client;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;

import commoble.morered.soldering.SolderingMenu;
import commoble.morered.soldering.SolderingRecipe;
import commoble.morered.soldering.SolderingRecipeButtonPacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.PacketDistributor;

public class SolderingScreen extends AbstractContainerScreen<SolderingMenu>
{
	public static final ResourceLocation TRADING_SCREEN = ResourceLocation.withDefaultNamespace("textures/gui/container/villager.png");
	public static final ResourceLocation CRAFTING_SCREEN = ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
	
	public static final int SCROLLPANEL_X = 4;
	public static final int SCROLLPANEL_Y = 17;
	public static final int SCROLLPANEL_WIDTH = 97;
	public static final int SCROLLPANEL_HEIGHT = 142;
	
	private SolderingScrollPanel scrollPanel;

	public SolderingScreen(SolderingMenu screenContainer, Inventory inv, Component titleIn)
	{
		super(screenContainer, inv, titleIn);
		int vanillaImageWidth = this.imageWidth;
		this.imageWidth = 276;
		this.imageHeight = 166;
		this.inventoryLabelX += (this.imageWidth - vanillaImageWidth);
	}
	
	@Override
	public void init()
	{
		super.init();
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		ClientLevel world = this.minecraft.level;
		List<RecipeHolder<SolderingRecipe>> recipes = world != null ? ClientProxy.getAllSolderingRecipes(world.getRecipeManager(), world.registryAccess()) : ImmutableList.of();
		this.scrollPanel = new SolderingScrollPanel(this.minecraft, this, recipes, xStart + SCROLLPANEL_X, yStart + SCROLLPANEL_Y, SCROLLPANEL_WIDTH, SCROLLPANEL_HEIGHT);
		this.addWidget(this.scrollPanel);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
		if (this.scrollPanel != null)
		{
			this.scrollPanel.render(graphics, mouseX, mouseY, 0);
		}
		this.renderTooltip(graphics, mouseX, mouseY);
	}
	
	public void renderItemStack(GuiGraphics graphics, ItemStack stack, int x, int y)
	{
        graphics.renderFakeItem(stack, x, y);
        graphics.renderItemDecorations(this.font, stack, x, y);
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem())
		{
			graphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
		}
		else if (this.scrollPanel != null && !this.scrollPanel.tooltipItem.isEmpty())
		{
			graphics.renderTooltip(this.font, this.scrollPanel.tooltipItem, mouseX, mouseY);
		}

	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
	{
		// use the trading window as the main background
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		
		graphics.blit(TRADING_SCREEN, xStart,  yStart, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
		
		// stretch the arrow over the crafting slot background
		int arrowU = 186;
		int arrowV = 36;
		int arrowWidth = 14;
		int arrowHeight = 18;
		int tiles = 4;
		int arrowScreenX = xStart + arrowU - tiles*arrowWidth;
		int arrowScreenY = yStart + arrowV;
		int blitWidth = arrowWidth*tiles;
		graphics.blit(TRADING_SCREEN, arrowScreenX, arrowScreenY, blitWidth, arrowHeight, arrowU, arrowV, arrowWidth, arrowHeight, 512, 256);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
	{
		// ContainerScreen doesn't delegate the mouse-dragged event to its children by default, so we need to do it here
		return (this.scrollPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
	}

	public static class RecipeButton extends ExtendedButton
	{
		private final int baseY;
		private final SolderingScreen screen;
		private final RecipeHolder<SolderingRecipe> recipe;
		public ItemStack tooltipItem = ItemStack.EMPTY;

		public RecipeButton(SolderingScreen screen, RecipeHolder<SolderingRecipe> recipe, int x, int y, int width)
		{
			super(x, y, width, getHeightForRecipe(recipe.value()), Component.literal(""), button -> onButtonClicked(screen.menu, recipe));
			this.baseY = y;
			this.screen = screen;
			this.recipe = recipe;
		}
		
		public static void onButtonClicked(SolderingMenu container, RecipeHolder<SolderingRecipe> recipe)
		{
			PacketDistributor.sendToServer(new SolderingRecipeButtonPacket(recipe.id()));
			container.attemptRecipeAssembly(recipe.value());
		}
		
		public static int getHeightForRecipe(SolderingRecipe recipe)
		{
			int rows = 1 + (recipe.ingredients().size()-1) / 3;
			return (rows*18)+5; // 2 padding on top, 3 on bottom
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
		{
			return false;
		}

	    /**
	     * Draws this button to the screen.
	     */
	    @Override
	    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial)
	    {
	    	this.tooltipItem = ItemStack.EMPTY;
	        if (this.visible)
	        {
	        	int thisX = this.getX();
	        	int thisY = this.getY();
	            super.renderWidget(graphics, mouseX, mouseY, partial);
	            List<SizedIngredient> ingredients = this.recipe.value().ingredients();
	            int ingredientCount = ingredients.size();
	            // render ingredients
	            for (int ingredientIndex=0; ingredientIndex<ingredientCount; ingredientIndex++)
	            {
	            	ItemStack stack = getIngredientVariant(ingredients.get(ingredientIndex).getItems());

	            	int itemRow = ingredientIndex / 3;
	            	int itemColumn = ingredientIndex % 3;
	            	int itemOffsetX = 2 + itemColumn*18;
	            	int itemOffsetY = 2 + itemRow*18;
	            	int itemX = thisX + itemOffsetX;
	            	int itemY = thisY + itemOffsetY;
	            	int itemEndX = itemX + 18;
	            	int itemEndY = itemY + 18;
	            	this.screen.renderItemStack(graphics, stack, itemX, itemY);
	            	if (mouseX >= itemX && mouseX < itemEndX && mouseY >= itemY && mouseY < itemEndY)
	            	{
	            		this.tooltipItem = stack;
	            	}
	            }
	            if (ingredientCount > 0)
	            {
		            // render helpy crafty arrow // arrow is 10x9 on the villager trading gui texture
	            	int extraIngredientRows = (ingredientCount-1)/3; //0 if 3 ingredients, 1 if 4-6 ingredients, 2 if 7-9 ingredients, etc
		            int arrowX = thisX + 2 + (18*3) + 4;
		            int arrowY = thisY + 2 + 4 + 9*extraIngredientRows;
		            int arrowWidth = 10;
		            int arrowHeight = 9;
		            int arrowU = 15;
		            int arrowV = 171;
		    		RenderSystem.setShaderTexture(0, TRADING_SCREEN);
		            graphics.blit(TRADING_SCREEN, arrowX, arrowY, arrowU, arrowV, arrowWidth, arrowHeight, 512, 256);
		            
		            // render the output item
		            ItemStack outputStack = this.recipe.value().getResultItem(this.screen.minecraft.level.registryAccess());
		            if (!outputStack.isEmpty())
		            {
			            int itemX = thisX + 2 + (18*4);
			            int itemY = thisY + 2 + 9*extraIngredientRows;
		            	int itemEndX = itemX + 18;
		            	int itemEndY = itemY + 18;
		            	this.screen.renderItemStack(graphics, outputStack, itemX, itemY);
		            	if (mouseX >= itemX && mouseX < itemEndX && mouseY >= itemY && mouseY < itemEndY)
		            	{
		            		this.tooltipItem = outputStack;
		            	}
		            }
		            
	            }
	        }
		}
	    
	    public static ItemStack getIngredientVariant(ItemStack[] variants)
	    {
	    	int variantCount = variants.length;
	    	if (variantCount > 0)
	    	{
            	// if this ingredient has multiple stacks, cycle through them
            	int variantIndex = (int)((Util.getMillis() / 1000L) / variantCount);
            	return variants[Mth.clamp(variantIndex, 0, variantCount - 1)];
	    	}
	    	else
	    	{
	    		return ItemStack.EMPTY;
	    	}
	    }

		public void scrollButton(int currentScrollAmount)
		{
			this.setY(this.baseY - currentScrollAmount);
		}
		
		public void onClickButton()
		{
			
		}
	}

	public static class SolderingScrollPanel extends ScrollPanel implements GuiEventListener
	{
		private List<RecipeButton> buttons = new ArrayList<>();
		public ItemStack tooltipItem = ItemStack.EMPTY;
		public final int totalButtonHeight;
		
		public SolderingScrollPanel(Minecraft client, SolderingScreen screen, List<RecipeHolder<SolderingRecipe>> recipes, int left, int top, int width, int height)
		{
			super(client, width, height, top, left);
			int buttonWidth = 90;
			
			int totalButtonHeight = 0;
			Level world = client.level;
			if (world != null)
			{
				for (RecipeHolder<SolderingRecipe> recipe : ClientProxy.getAllSolderingRecipes(world.getRecipeManager(), world.registryAccess()))
				{
					RecipeButton recipeButton = new RecipeButton(screen, recipe, left, top + totalButtonHeight, buttonWidth);
					this.buttons.add(recipeButton);
					totalButtonHeight += recipeButton.getHeight();
				}
			}
			this.totalButtonHeight = totalButtonHeight;
		}
		
		@Override
		public List<? extends GuiEventListener> children()
		{
			return this.buttons;
		}

		@Override
		protected int getContentHeight()
		{
			return this.totalButtonHeight;
		}

		@Override
		protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY)
		{
	    	this.tooltipItem = ItemStack.EMPTY;
			for (RecipeButton button : this.buttons)
			{
				button.scrollButton((int) this.scrollDistance);
				button.render(graphics, mouseX, mouseY, 0);
				if (!button.tooltipItem.isEmpty())
				{
					this.tooltipItem = button.tooltipItem;
				}
			}
		}


		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button)
		{
			return super.mouseClicked(mouseX, mouseY, button);
		}

		@Override
		public NarrationPriority narrationPriority()
		{
			return NarrationPriority.NONE;
		}

		@Override
		public void updateNarration(NarrationElementOutput output)
		{
		}
	}
}
