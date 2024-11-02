package net.commoble.morered;

import net.commoble.morered.soldering.SolderingRecipesPacket;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.network.PacketDistributor;

public class MixinCallbacks
{
	/**
	 * @param playerList playerList
	 * @deprecated remove after neoforge moves OnDatapackSyncEvent to after tags on reload
	 */
	@Deprecated
	public static void afterReloadResources(PlayerList playerList)
	{
		PacketDistributor.sendToAllPlayers(SolderingRecipesPacket.create(playerList.getServer().getRecipeManager()));
	}
}
