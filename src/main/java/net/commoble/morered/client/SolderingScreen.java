package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.commoble.morered.soldering.SolderingMenu;
import net.commoble.morered.soldering.SolderingRecipe;
import net.commoble.morered.soldering.SolderingRecipe.SolderingRecipeHolder;
import net.commoble.morered.soldering.SolderingRecipeButtonPacket;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public class SolderingScreen extends AbstractContainerScreen<SolderingMenu>
{
	public static final Identifier TRADING_SCREEN = Identifier.withDefaultNamespace("textures/gui/container/villager.png");
	public static final Identifier CRAFTING_SCREEN = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
	
	public static final int SEARCHBOX_X = 4;
	public static final int SEARCHBOX_Y = 17;
	public static final int SEARCHBOX_WIDTH = 97;
	public static final int SEARCHBOX_HEIGHT = 12;
	public static final int SCROLLPANEL_X = 4;
	public static final int SCROLLPANEL_Y = SEARCHBOX_Y + SEARCHBOX_HEIGHT;
	public static final int SCROLLPANEL_WIDTH = 97;
	public static final int SCROLLPANEL_HEIGHT = 142 - SEARCHBOX_HEIGHT;
	
	private EditBox searchBox;
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
		List<SolderingRecipeHolder> recipes = world != null ? ClientProxy.getAllSolderingRecipes() : List.of();
		this.searchBox = new EditBox(this.font, xStart + SEARCHBOX_X, yStart + SEARCHBOX_Y, SEARCHBOX_WIDTH, SEARCHBOX_HEIGHT, Component.literal("a"));
        this.searchBox.setTextColor(0xFFFFFFFF); // same as creative search
        this.searchBox.setFocused(true); // start with searchbox focused
		this.addWidget(this.searchBox);
		this.scrollPanel = new SolderingScrollPanel(this.minecraft, this, recipes, xStart + SCROLLPANEL_X, yStart + SCROLLPANEL_Y, SCROLLPANEL_WIDTH, SCROLLPANEL_HEIGHT);
        this.searchBox.setResponder(this.scrollPanel::updateButtons);
		this.addWidget(this.scrollPanel);
		this.setInitialFocus(this.searchBox);
	}

	@Override
	public boolean charTyped(CharacterEvent event)
	{
		return this.searchBox.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event)
	{
		return this.searchBox.keyPressed(event)
			|| (this.searchBox.isFocused() && event.key() != 256)
			|| super.keyPressed(event);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		super.render(graphics, mouseX, mouseY, partialTicks);
		if (this.searchBox != null)
		{
			graphics.pose().pushMatrix();
			this.searchBox.render(graphics, mouseX, mouseY, 0);
			graphics.pose().popMatrix();
		}
		if (this.scrollPanel != null)
		{
			graphics.pose().pushMatrix();
			this.scrollPanel.render(graphics, mouseX, mouseY, 0);
			graphics.pose().popMatrix();
		}
		// the scrollpanel messes with the rendering of the labels, so do those here instead
		graphics.pose().pushMatrix();
		graphics.pose().translate(this.leftPos, this.topPos);
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
		graphics.pose().popMatrix();
		this.renderTooltip(graphics, mouseX, mouseY);
	}

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
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
            graphics.setTooltipForNextFrame(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
		}
		else if (this.scrollPanel != null && !this.scrollPanel.tooltipItem.isEmpty())
		{
			graphics.setTooltipForNextFrame(this.font, this.scrollPanel.tooltipItem, mouseX, mouseY);
		}

	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
	{
		// use the trading window as the main background
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		
		// screenStartX, screenStartY, textureU, textureV, blitWidth, blitHeight, fileWidth, fileHeight
		graphics.blit(RenderPipelines.GUI_TEXTURED, TRADING_SCREEN, xStart,  yStart, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
		
		// stretch the arrow over the crafting slot background
		int arrowU = 186;
		int arrowV = 36;
		int arrowWidth = 14;
		int arrowHeight = 18;
		int tiles = 4;
		int arrowScreenX = xStart + arrowU - tiles*arrowWidth;
		int arrowScreenY = yStart + arrowV;
		int blitWidth = arrowWidth*tiles;
		// screenStartX, screenStartY, textureU, textureV, blitToWidth, blitToHeight, blitFromWidth, blitFromHeight, fileWidth, fileHeight
		graphics.blit(RenderPipelines.GUI_TEXTURED, TRADING_SCREEN, arrowScreenX, arrowScreenY, arrowU, arrowV,  blitWidth, arrowHeight, arrowWidth, arrowHeight, 512, 256);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
	{
		// ContainerScreen doesn't delegate the mouse-dragged event to its children by default, so we need to do it here
		return (this.scrollPanel.mouseDragged(event,dx,dy) || super.mouseDragged(event,dx,dy));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
	{
		return this.scrollPanel.mouseScrolled(mouseX, mouseY, deltaX, deltaY)
			|| super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	public static class RecipeButton extends ExtendedButton
	{
		private final int baseY;
		private final SolderingScreen screen;
		private final SolderingRecipeHolder recipe;
		public ItemStack tooltipItem = ItemStack.EMPTY;

		public RecipeButton(SolderingScreen screen, SolderingRecipeHolder recipe, int x, int y, int width)
		{
			super(x, y, width, getHeightForRecipe(recipe.recipe()), Component.literal(""), button -> onButtonClicked(screen.menu, recipe));
			this.baseY = y;
			this.screen = screen;
			this.recipe = recipe;
		}
		
		public static void onButtonClicked(SolderingMenu container, SolderingRecipeHolder recipe)
		{
			Minecraft.getInstance().getConnection().send(new SolderingRecipeButtonPacket(recipe.id()));
			container.attemptRecipeAssembly(recipe.recipe());
		}
		
		public static int getHeightForRecipe(SolderingRecipe recipe)
		{
			int rows = 1 + (recipe.ingredients().size()-1) / 3;
			return (rows*18)+5; // 2 padding on top, 3 on bottom
		}

		@Override
		public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
		{
			return false;
		}

	    /**
	     * Draws this button to the screen.
	     */
	    @Override
	    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partial)
	    {
	    	this.tooltipItem = ItemStack.EMPTY;
	        if (this.visible)
	        {
	        	int thisX = this.getX();
	        	int thisY = this.getY();
	            super.renderWidget(graphics, mouseX, mouseY, partial);
	            List<SizedIngredient> ingredients = this.recipe.recipe().ingredients();
	            int ingredientCount = ingredients.size();
	            // render ingredients
	            for (int ingredientIndex=0; ingredientIndex<ingredientCount; ingredientIndex++)
	            {
	            	ItemStack stack = getIngredientVariant(ingredients.get(ingredientIndex));
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
		            graphics.blit(RenderPipelines.GUI_TEXTURED, TRADING_SCREEN, arrowX, arrowY, arrowU, arrowV, arrowWidth, arrowHeight, 512, 256);
		            
		            // render the output item
		            ItemStack outputStack = this.recipe.recipe().result();
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
	    
	    public static ItemStack getIngredientVariant(SizedIngredient sizedIngredient)
	    {
	    	List<Holder<Item>> variants = sizedIngredient.ingredient().getValues().stream().toList();
	    	int variantCount = variants.size();
	    	if (variantCount > 0)
	    	{
            	// if this ingredient has multiple stacks, cycle through them
            	int variantIndex = (int)((Util.getMillis() / 1000L) % variantCount);
            	return new ItemStack(variants.get(variantIndex).value(), sizedIngredient.count());
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

	public class SolderingScrollPanel extends ScrollPanel implements GuiEventListener
	{
		private List<RecipeButton> buttons = new ArrayList<>();
		public ItemStack tooltipItem = ItemStack.EMPTY;
		public int totalButtonHeight;
		
		public SolderingScrollPanel(Minecraft client, SolderingScreen screen, List<SolderingRecipeHolder> recipes, int left, int top, int width, int height)
		{
			super(client, width, height, top, left);
			this.updateButtons("");
		}
		
		public void updateButtons(String searchText)
		{
			List<RecipeButton> buttons = new ArrayList<>();
			String upperSearchText = searchText.toUpperCase(Locale.ROOT);
			int buttonWidth = 90;
			int totalButtonHeight = 0;
			for (SolderingRecipeHolder recipe : ClientProxy.getAllSolderingRecipes())
			{
				if (I18n.get(recipe.recipe().result().getItem().getDescriptionId()).toUpperCase(Locale.ROOT).contains(upperSearchText))
				{
					RecipeButton recipeButton = new RecipeButton(SolderingScreen.this, recipe, left, top + totalButtonHeight, buttonWidth);
					buttons.add(recipeButton);
					totalButtonHeight += recipeButton.getHeight();
				}
			}
			this.totalButtonHeight = totalButtonHeight;
			this.buttons = buttons;
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
		protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, int mouseX, int mouseY)
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
		public boolean mouseClicked(MouseButtonEvent event, boolean jellybiscuits)
		{
			return super.mouseClicked(event, jellybiscuits);
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
