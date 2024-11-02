package net.commoble.morered.soldering;

import java.util.List;

import net.commoble.morered.MoreRed;
import net.commoble.morered.client.ClientProxy;
import net.commoble.morered.soldering.SolderingRecipe.SolderingRecipeHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SolderingRecipesPacket(List<SolderingRecipeHolder> recipes) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SolderingRecipesPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.id("soldering_recipes"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SolderingRecipesPacket> STREAM_CODEC = SolderingRecipe.SolderingRecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list())
		.map(SolderingRecipesPacket::new, SolderingRecipesPacket::recipes);
	
	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> this.handleThreadsafe(context));
	}
	
	public void handleThreadsafe(IPayloadContext context)
	{
		ClientProxy.updateSolderingRecipes(this.recipes);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
	
	public static SolderingRecipesPacket create(RecipeManager recipeManager)
	{
		return new SolderingRecipesPacket(recipeManager
			.recipeMap()
			.byType(MoreRed.get().solderingRecipeType.get())
			.stream()
			.map(holder -> new SolderingRecipeHolder(holder.id().location(), holder.value()))
			.toList());
	}
}
