package commoble.morered.gatecrafting_plinth;

import java.util.Optional;
import java.util.function.Function;

import commoble.morered.BlockRegistrar;
import commoble.morered.ContainerRegistrar;
import commoble.morered.RecipeRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

public class GatecraftingContainer extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT_ID = 0;
    public static final int FIRST_PLAYER_INVENTORY_SLOT_ID = OUTPUT_SLOT_ID + 1;
    public static final int PLAYER_INVENTORY_SLOT_ROWS = 4;
    public static final int PLAYER_INVENTORY_SLOT_COLUMNS = 9;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_SLOT_ROWS * PLAYER_INVENTORY_SLOT_COLUMNS;
    public static final int FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID =
            FIRST_PLAYER_INVENTORY_SLOT_ID + (PLAYER_INVENTORY_SLOT_ROWS - 1) * PLAYER_INVENTORY_SLOT_COLUMNS;

    /**
     * The player that opened the container
     **/
    private final Player player;
    /**
     * This is based on the position of the block the container was opened from (or the position of the player if no
     * block was involved)
     **/
    private final ContainerLevelAccess positionInWorld;
    public final SimpleContainer craftResult = new SimpleContainer(1);

    public Optional<Recipe<CraftingContainer>> currentRecipe = Optional.empty();


    public static GatecraftingContainer getClientContainer(int id, Inventory inventory,
                                                           FriendlyByteBuf friendlyByteBuf) {
        return new GatecraftingContainer(id, BlockPos.ZERO, inventory);
    }

    public static MenuProvider getServerContainerProvider(BlockPos pos) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return null;
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new GatecraftingContainer(id, pos, inventory);
            }
        };
    }

    protected GatecraftingContainer(int id, BlockPos pos, Inventory playerInventory) {
        super(ContainerRegistrar.GATECRAFTING.get(), id);
        this.player = playerInventory.player;
        this.positionInWorld = ContainerLevelAccess.create(this.player.level, pos);

        // crafting output slot // apparently it's helpful to do this first
//		this.addSlot(new GatecraftingResultSlot(this, playerInventory.player, this.craftingInventory, this
//		.craftResult, OUTPUT_SLOT_ID, inputOffsetX + 94, inputOffsetY + slotHeight));
        this.addSlot(new GatecraftingResultSlot(this, this.craftResult, OUTPUT_SLOT_ID, 220, 38));

        // add player inventory
        for (int column = 0; column < 3; ++column) {
            for (int row = 0; row < 9; ++row) {
                this.addSlot(new Slot(playerInventory, row + column * 9 + 9, 108 + row * 18, 84 + column * 18));
            }
        }

        // player inventory hotbar slots
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 108 + i * 18, 142));
        }
    }

    /**
     * Determines whether supplied player can use this container
     */
    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(this.positionInWorld, playerIn, BlockRegistrar.GATECRAFTING_PLINTH.get());
    }

    /**
     * Called to determine if the current slot is valid for the stack merging
     * (double-click) code. The stack passed in is null for the initial slot that
     * was double-clicked.
     */
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return false;
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this
     * moves the stack between the player inventory and the other inventory(s).
     */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int slotIndex) {
        ItemStack copiedStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            copiedStack = stackInSlot.copy();
            // if the output slot was clicked
            if (slotIndex == OUTPUT_SLOT_ID) {
                if (!this.moveItemStackTo(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID,
                        FIRST_PLAYER_INVENTORY_SLOT_ID + PLAYER_INVENTORY_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, copiedStack);
            }
            // if a player inventory slot was clicked, try to move it from the hotbar to the backpack or vice-versa
            else if (slotIndex >= FIRST_PLAYER_INVENTORY_SLOT_ID) {
                // if it was not a hotbar slot, try to merge it into the hotbar first
                if (slotIndex < FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID) {
                    if (!this.moveItemStackTo(stackInSlot, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID,
                            FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID + PLAYER_INVENTORY_SLOT_COLUMNS, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // if it was a hotbar slot, try to merge it to the player's backpack
                else if (slotIndex < FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID + PLAYER_INVENTORY_SLOT_COLUMNS && !this.moveItemStackTo(stackInSlot, FIRST_PLAYER_INVENTORY_SLOT_ID, FIRST_PLAYER_INVENTORY_HOTBAR_SLOT_ID, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == copiedStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stackInSlot);
        }

        return copiedStack;
    }


    public void onPlayerChoseRecipe(ResourceLocation recipeID) {
        this.attemptRecipeAssembly(RecipeRegistrar.getGatecraftingRecipe(this.player.level.getRecipeManager(),
                recipeID));
    }

    /**
     * Attempts to assemble the given recipe and updates crafting result if successful
     *
     * @param recipeHolder recipe holder
     */
    public void attemptRecipeAssembly(Optional<Recipe<CraftingContainer>> recipeHolder) {
        Optional<Recipe<CraftingContainer>> filteredRecipe =
                recipeHolder.filter(recipe -> recipe.getType() == RecipeRegistrar.GATECRAFTING_RECIPE_TYPE
                        && GatecraftingRecipe.doesPlayerHaveIngredients(this.player.getInventory(), recipe));
        this.updateRecipeAndResult(filteredRecipe);
    }

    public void updateRecipeAndResult(Optional<Recipe<CraftingContainer>> recipeHolder) {
        this.currentRecipe = recipeHolder;
        this.craftResult.setItem(0, recipeHolder.map(recipe -> recipe.getResultItem().copy()).orElse(ItemStack.EMPTY));
    }
}
