package commoble.morered.gatecrafting_plinth;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

/** Packet sent from client to server when client clicks a recipe button in the gatecrafting screen **/
public class GatecraftingRecipeButtonPacket
{
	public final ResourceLocation recipeID;
	
	public GatecraftingRecipeButtonPacket(ResourceLocation recipeID)
	{
		this.recipeID = recipeID;
	}
	
	public void write(PacketBuffer packet)
	{
		packet.writeResourceLocation(this.recipeID);
	}
	
	public static GatecraftingRecipeButtonPacket read(PacketBuffer packet)
	{
		return new GatecraftingRecipeButtonPacket(packet.readResourceLocation());
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		contextGetter.get().enqueueWork(() -> this.handleThreadsafe(contextGetter.get()));
		contextGetter.get().setPacketHandled(true);
	}
	
	public void handleThreadsafe(NetworkEvent.Context context)
	{
		ServerPlayerEntity player = context.getSender();
		if (player != null)
		{
			Container container = player.containerMenu;
			if (container instanceof GatecraftingContainer)
			{
				((GatecraftingContainer)container).onPlayerChoseRecipe(this.recipeID);
			}
		}
	}
}
