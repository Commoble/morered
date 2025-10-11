package net.commoble.morered;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.commoble.morered.client.ClientProxy;
import net.minecraft.world.entity.player.Player;

/**
 * Class that holds information on players on the server
 * Except where otherwise specified, should only be referenced from the server thread
 * Data should be considered temporary, no data is saved on world save
 */
public class PlayerData
{
	// players that are currently holding the sprint key
	private static final Set<UUID> sprintingPlayers = new HashSet<UUID>();
	
	public static void setSprinting(UUID id, boolean isSprinting)
	{
		if (isSprinting)
		{
			sprintingPlayers.add(id);
		}
		else
		{
			sprintingPlayers.remove(id);
		}
	}
	
	public static boolean getSprinting(UUID id)
	{
		return sprintingPlayers.contains(id);
	}
	
	/**
	 * Returns whether an interacting player is sprinting.
	 * If called on the server, uses the global player data.
	 * If called on the client, returns true if the player is the client player and sprinting, otherwise returns false.
	 * @param player Player
	 * @return true if the player is a sprinting server player, or is the client's player and sprinting (never returns true for RemotePlayer)
	 */
	public static boolean getCommonInteractionSprinting(Player player)
	{
		if (player.level().isClientSide())
		{
			return ClientProxy.getSprintingIfClientPlayer(player);
		}
		else
		{
			return getSprinting(player.getUUID());
		}
	}
}
