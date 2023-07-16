package commoble.morered.soldering;

import java.util.function.Supplier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Packet sent from client to server when client clicks a recipe button in the soldering screen **/
public class SolderingRecipeButtonPacket
{
	public final ResourceLocation recipeID;
	
	public SolderingRecipeButtonPacket(ResourceLocation recipeID)
	{
		this.recipeID = recipeID;
	}
	
	public void write(FriendlyByteBuf packet)
	{
		packet.writeResourceLocation(this.recipeID);
	}
	
	public static SolderingRecipeButtonPacket read(FriendlyByteBuf packet)
	{
		return new SolderingRecipeButtonPacket(packet.readResourceLocation());
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		contextGetter.get().enqueueWork(() -> this.handleThreadsafe(contextGetter.get()));
		contextGetter.get().setPacketHandled(true);
	}
	
	public void handleThreadsafe(NetworkEvent.Context context)
	{
		ServerPlayer player = context.getSender();
		if (player != null)
		{
			AbstractContainerMenu container = player.containerMenu;
			if (container instanceof SolderingMenu)
			{
				((SolderingMenu)container).onPlayerChoseRecipe(this.recipeID);
			}
		}
	}
}
