package commoble.morered.gatecrafting_plinth;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet sent from client to server when client clicks a recipe button in the gatecrafting screen
 **/
public class GatecraftingRecipeButtonPacket {
    public final ResourceLocation recipeID;

    public GatecraftingRecipeButtonPacket(ResourceLocation recipeID) {
        this.recipeID = recipeID;
    }

    public void write(FriendlyByteBuf packet) {
        packet.writeResourceLocation(this.recipeID);
    }

    public static GatecraftingRecipeButtonPacket read(FriendlyByteBuf packet) {
        return new GatecraftingRecipeButtonPacket(packet.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        contextGetter.get().enqueueWork(() -> this.handleThreadsafe(contextGetter.get()));
        contextGetter.get().setPacketHandled(true);
    }

    public void handleThreadsafe(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            AbstractContainerMenu container = player.containerMenu;
            if (container instanceof GatecraftingContainer) {
                ((GatecraftingContainer) container).onPlayerChoseRecipe(this.recipeID);
            }
        }
    }
}
