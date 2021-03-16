package commoble.morered.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import commoble.morered.BlockRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.RecipeRegistrar;
import commoble.morered.gatecrafting_plinth.GatecraftingContainer;
import commoble.morered.gatecrafting_plinth.GatecraftingRecipeButtonPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;

public class GatecraftingScreen extends ContainerScreen<GatecraftingContainer>
{
	public static final ResourceLocation TRADING_SCREEN = new ResourceLocation("minecraft:textures/gui/container/villager2.png");
	public static final ResourceLocation CRAFTING_SCREEN = new ResourceLocation("minecraft:textures/gui/container/crafting_table.png");
	
	public static final int SCROLLPANEL_X = 4;
	public static final int SCROLLPANEL_Y = 17;
	public static final int SCROLLPANEL_WIDTH = 97;
	public static final int SCROLLPANEL_HEIGHT = 142;
	
	private final String name;
	
	private GatecraftingScrollPanel scrollPanel;

	public GatecraftingScreen(GatecraftingContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
	{
		super(screenContainer, inv, titleIn);
		this.imageWidth = 276;
		this.imageHeight = 166;
		this.name = new TranslationTextComponent(BlockRegistrar.GATECRAFTING_PLINTH.get().getDescriptionId()).getString();
	}
	
	@Override
	public void init()
	{
		super.init();
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		ClientWorld world = this.minecraft.level;
		List<IRecipe<CraftingInventory>> recipes = world != null ? RecipeRegistrar.getAllGatecraftingRecipes(world.getRecipeManager()) : ImmutableList.of();
		this.scrollPanel = new GatecraftingScrollPanel(this.minecraft, this, recipes, xStart + SCROLLPANEL_X, yStart + SCROLLPANEL_Y, SCROLLPANEL_WIDTH, SCROLLPANEL_HEIGHT);
		this.children.add(this.scrollPanel);
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks)
	{
		this.renderBackground(matrix);
		super.render(matrix, mouseX, mouseY, partialTicks);
		if (this.scrollPanel != null)
		{
			this.scrollPanel.render(matrix, mouseX, mouseY, 0);
		}
		this.renderTooltip(matrix, mouseX, mouseY);
	}
	
	public void renderItemStack(ItemStack stack, int x, int y)
	{
        this.itemRenderer.renderAndDecorateItem(stack, x, y);
        this.itemRenderer.renderGuiItemDecorations(this.font, stack, x, y);
	}

	@Override
	protected void renderTooltip(MatrixStack matrix, int mouseX, int mouseY)
	{
		if (this.minecraft.player.inventory.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem())
		{
			this.renderTooltip(matrix, this.hoveredSlot.getItem(), mouseX, mouseY);
		}
		else if (this.scrollPanel != null && !this.scrollPanel.tooltipItem.isEmpty())
		{
			this.renderTooltip(matrix, this.scrollPanel.tooltipItem, mouseX, mouseY);
		}

	}


	@Override
	protected void renderLabels(MatrixStack matrix, int mouseX, int mouseY)
	{
		String playerName = this.inventory.getName().getString();
		this.font.draw(matrix, this.name, this.imageWidth/2 - this.font.width(this.name)/2, 6, 4210752);	// y-value and color from dispenser, etc
		this.font.draw(matrix, playerName, 107, this.imageHeight-96+2, 4210752);
	}

	@Override
	protected void renderBg(MatrixStack matrix, float partialTicks, int mouseX, int mouseY)
	{
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		int xStart = (this.width - this.imageWidth) / 2;
		int yStart = (this.height - this.imageHeight) / 2;
		
		// use the trading window as the main background
		this.minecraft.getTextureManager().bind(TRADING_SCREEN);
		AbstractGui.blit(matrix, xStart,  yStart, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
		
		// stretch the arrow over the crafting slot background
		int arrowU = 186;
		int arrowV = 36;
		int arrowWidth = 14;
		int arrowHeight = 18;
		int tiles = 4;
		int arrowScreenX = xStart + arrowU - tiles*arrowWidth;
		int arrowScreenY = yStart + arrowV;
		int blitWidth = arrowWidth*tiles;
		AbstractGui.blit(matrix, arrowScreenX, arrowScreenY, blitWidth, arrowHeight, arrowU, arrowV, arrowWidth, arrowHeight, 512, 256);
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
		private final GatecraftingScreen screen;
		private final IRecipe<CraftingInventory> recipe;
		public ItemStack tooltipItem = ItemStack.EMPTY;

		public RecipeButton(GatecraftingScreen screen, IRecipe<CraftingInventory> recipe, int x, int y, int width)
		{
			super(x, y, width, getHeightForRecipe(recipe), new StringTextComponent(""), button -> onButtonClicked(screen.menu, recipe));
			this.baseY = y;
			this.screen = screen;
			this.recipe = recipe;
		}
		
		public static void onButtonClicked(GatecraftingContainer container, IRecipe<CraftingInventory> recipe)
		{
			MoreRed.CHANNEL.sendToServer(new GatecraftingRecipeButtonPacket(recipe.getId()));
			container.attemptRecipeAssembly(Optional.of(recipe));
		}
		
		public static int getHeightForRecipe(IRecipe<?> recipe)
		{
			int rows = 1 + (recipe.getIngredients().size()-1) / 3;
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
	    public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partial)
	    {
	    	this.tooltipItem = ItemStack.EMPTY;
	        if (this.visible)
	        {
	            super.renderButton(matrix, mouseX, mouseY, partial);
	            NonNullList<Ingredient> ingredients = this.recipe.getIngredients();
	            int ingredientCount = ingredients.size();
	            // render ingredients
	            for (int ingredientIndex=0; ingredientIndex<ingredientCount; ingredientIndex++)
	            {
	            	ItemStack stack = getIngredientVariant(ingredients.get(ingredientIndex).getItems());

	            	int itemRow = ingredientIndex / 3;
	            	int itemColumn = ingredientIndex % 3;
	            	int itemOffsetX = 2 + itemColumn*18;
	            	int itemOffsetY = 2 + itemRow*18;
	            	int itemX = this.x + itemOffsetX;
	            	int itemY = this.y + itemOffsetY;
	            	int itemEndX = itemX + 18;
	            	int itemEndY = itemY + 18;
	            	this.screen.renderItemStack(stack, itemX, itemY);
	            	if (mouseX >= itemX && mouseX < itemEndX && mouseY >= itemY && mouseY < itemEndY)
	            	{
	            		this.tooltipItem = stack;
	            	}
	            }
	            if (ingredientCount > 0)
	            {
		            // render helpy crafty arrow // arrow is 10x9 on the villager trading gui texture
	            	int extraIngredientRows = (ingredientCount-1)/3; //0 if 3 ingredients, 1 if 4-6 ingredients, 2 if 7-9 ingredients, etc
		            int arrowX = this.x + 2 + (18*3) + 4;
		            int arrowY = this.y + 2 + 4 + 9*extraIngredientRows;
		            int arrowWidth = 10;
		            int arrowHeight = 9;
		            int arrowU = 15;
		            int arrowV = 171;
		            this.screen.minecraft.textureManager.bind(TRADING_SCREEN);
		            Screen.blit(matrix, arrowX, arrowY, arrowU, arrowV, arrowWidth, arrowHeight, 512, 256);
		            
		            // render the output item
		            ItemStack outputStack = this.recipe.getResultItem();
		            if (!outputStack.isEmpty())
		            {
			            int itemX = this.x + 2 + (18*4);
			            int itemY = this.y + 2 + 9*extraIngredientRows;
		            	int itemEndX = itemX + 18;
		            	int itemEndY = itemY + 18;
		            	this.screen.renderItemStack(outputStack, itemX, itemY);
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
            	return variants[MathHelper.clamp(variantIndex, 0, variantCount - 1)];
	    	}
	    	else
	    	{
	    		return ItemStack.EMPTY;
	    	}
	    }

		public void scrollButton(int currentScrollAmount)
		{
			this.y = this.baseY - currentScrollAmount;
		}
		
		public void onClickButton()
		{
			
		}
	}

	public static class GatecraftingScrollPanel extends ScrollPanel
	{
		private List<RecipeButton> buttons = new ArrayList<>();
		public ItemStack tooltipItem = ItemStack.EMPTY;
		public final int totalButtonHeight;
		
		public GatecraftingScrollPanel(Minecraft client, GatecraftingScreen screen, List<IRecipe<CraftingInventory>> recipes, int left, int top, int width, int height)
		{
			super(client, width, height, top, left);
			int buttonWidth = 90;
			
			int totalButtonHeight = 0;
			World world = client.level;
			if (world != null)
			{
				for (IRecipe<CraftingInventory> recipe : RecipeRegistrar.getAllGatecraftingRecipes(world.getRecipeManager()))
				{
					RecipeButton recipeButton = new RecipeButton(screen, recipe, left, top + totalButtonHeight, buttonWidth);
					this.buttons.add(recipeButton);
					totalButtonHeight += recipeButton.getHeight();
				}
			}
			this.totalButtonHeight = totalButtonHeight;
		}
		
		@Override
		public List<? extends IGuiEventListener> children()
		{
			return this.buttons;
		}

		@Override
		protected int getContentHeight()
		{
			return this.totalButtonHeight;
		}

		@Override
		protected void drawPanel(MatrixStack matrix, int entryRight, int relativeY, Tessellator tess, int mouseX, int mouseY)
		{
	    	this.tooltipItem = ItemStack.EMPTY;
			for (RecipeButton button : this.buttons)
			{
				button.scrollButton((int) this.scrollDistance);
				button.render(matrix, mouseX, mouseY, 0);
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
	}
}
