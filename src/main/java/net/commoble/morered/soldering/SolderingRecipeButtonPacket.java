package net.commoble.morered.soldering;

import io.netty.buffer.ByteBuf;
import net.commoble.morered.MoreRed;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Packet sent from client to server when client clicks a recipe button in the soldering screen **/
public record SolderingRecipeButtonPacket(ResourceLocation recipeId) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SolderingRecipeButtonPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.getModRL("soldering_recipe_button"));
	public static final StreamCodec<ByteBuf, SolderingRecipeButtonPacket> STREAM_CODEC = ResourceLocation.STREAM_CODEC
		.map(SolderingRecipeButtonPacket::new, SolderingRecipeButtonPacket::recipeId);
	
	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> this.handleThreadsafe(context));
	}
	
	public void handleThreadsafe(IPayloadContext context)
	{
		if (context.player() instanceof ServerPlayer player)
		{
			AbstractContainerMenu container = player.containerMenu;
			if (container instanceof SolderingMenu menu)
			{
				menu.onPlayerChoseRecipe(this.recipeId);
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
