package commoble.morered.api;

import java.util.Map;

import commoble.morered.MoreRed;
import commoble.morered.wires.WireConnectors;
import net.minecraft.block.Block;

/**
 * This class contains some methods that can be used by other mods
 * to interface with More Red's features.
 * To safely refer to this class without creating hard dependencies,
 * create a delegate class (we can call this MoreRedProxy),
 * and then we can statically refer to it like <pre>
		if (ModList.get().isLoaded("morered"))
		{
			MoreRedProxy.registerWireConnectors();
		}
	</pre>
 * In this way, MoreRedProxy can safely refer to classes from More Red,
 * only causing them to be classloaded if the morered mod is loaded.
 */
public final class MoreRedAPI
{
	/**
	 * Retrieves the registry for wire connectabilities.
	 * During modloading, this map is mutable and thread-safe.
	 * This method is not safe to call during mod constructors.
	 * Blocks should be registered to this registry in FMLCommonSetupEvent.
	 * After modloading completes, the map becomes immutable.
	 * 
	 * Functions registered to this registry will be queried by wire blocks.
	 * If the function assigned to a given block returns true, then the wire block will
	 * A) supply power to that block when that block is adjacent
	 * B) receive power to that block when that block is adjacent
	 * C) render additional lines of wires attaching to that block.
	 * 
	 * It does not affect the result of WireBlock::canConnectRedstone.
	 * @return The wire connectability registry
	 */
	public static Map<Block, WireConnector> getWireConnectabilityRegistry()
	{
		return MoreRed.INSTANCE.getWireConnectabilities();
	}
	
	/**
	 * Gets the default wire connector for blocks that do not have WireConnector
	 * overrides registered to them.
	 * @return The default wire connector
	 */
	public static WireConnector getDefaultWireConnector()
	{
		return WireConnectors.DEFAULT_WIRE_CONNECTOR;
	}
}
