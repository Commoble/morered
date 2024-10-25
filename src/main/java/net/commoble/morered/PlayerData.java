package net.commoble.morered;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Class that holds information on players on the server
 * This should only be referenced from the server thread
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
}
