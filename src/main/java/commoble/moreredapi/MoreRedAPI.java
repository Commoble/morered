package commoble.moreredapi;

import commoble.moreredapi.internal.APIRegistries;
import commoble.moreredapi.internal.DefaultWireProperties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.Map;

/**
 * This class contains some methods that can be used by other mods
 * to interface with More Red's features.
 * To safely refer to this class without creating hard dependencies,
 * create a delegate class (we can call this MoreRedProxy),
 * and then we can statically refer to it like <pre>
 * if (ModList.get().isLoaded("morered"))
 * {
 * MoreRedProxy.registerWireConnectors();
 * }
 * </pre>
 * In this way, MoreRedProxy can safely refer to classes from More Red,
 * only causing them to be classloaded if the morered mod is loaded.
 */
public final class MoreRedAPI {
    /**
     * Capability instance for the Channeled Power Handler capability.
     * The default instance and default storage are not implemented and will probably cause crashes if they are used.
     */
    public static final Capability<ChanneledPowerSupplier> CHANNELED_POWER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /**
     * Retrieves the registry for wire connectabilities to red alloy wires and colored network cables.
     * During modloading, this map is mutable and thread-safe.
     * This method is not safe to call during mod constructors.
     * Blocks should be registered to this registry in FMLCommonSetupEvent.
     * After modloading completes, the map becomes immutable.
     * <p>
     * Functions registered to this registry will be queried by wire blocks.
     * If the function assigned to a given block returns true, then the wire block will
     * A) supply power to that block when that block is adjacent
     * B) receive power to that block when that block is adjacent
     * C) render additional lines of wires attaching to that block.
     * <p>
     * It does not affect the result of WireBlock::canConnectRedstone.
     *
     * @return The wire connectability registry
     */
    public static Map<Block, WireConnector> getWireConnectabilityRegistry() {
        return APIRegistries.getWireConnectabilities();
    }

    /**
     * Gets the default wire connector for blocks that do not have WireConnector
     * overrides registered to them.
     *
     * @return The default wire connector
     */
    public static WireConnector getDefaultWireConnector() {
        return DefaultWireProperties.DEFAULT_WIRE_CONNECTOR;
    }

    /**
     * Gets the registry of "expanded power" suppliers.
     * An expanded power supplier is defined here as a block that can supply redstone-like power in the [0,31] range
     * instead of [0,15].
     * This power behaviour is used by red alloy wires and colored network cables.
     * The default behaviour is to simply take the standard weak power of the block and double it.
     * This behaviour can be overridden by registering a function to this registry.
     * <p>
     * During modloading, this map is mutable and thread-safe.
     * This method is not safe to call during mod constructors.
     * Blocks should be registered to this registry in FMLCommonSetupEvent.
     * After modloading completes, the map becomes immutable.
     *
     * @return
     */
    public static Map<Block, ExpandedPowerSupplier> getExpandedPowerRegistry() {
        return APIRegistries.getExpandedPowerSuppliers();
    }

    public static ExpandedPowerSupplier getDefaultExpandedPowerSupplier() {
        return DefaultWireProperties.DEFAULT_EXPANDED_POWER_SUPPLIER;
    }

    /**
     * Retrieves the registry for wire connectabilities to bundled cables and colored network cables.
     * During modloading, this map is mutable and thread-safe.
     * This method is not safe to call during mod constructors.
     * Blocks should be registered to this registry in FMLCommonSetupEvent.
     * After modloading completes, the map becomes immutable.
     * <p>
     * Functions registered to this registry will be queried by cable blocks.
     * If the function assigned to a given block returns true, then the cable block will
     * A) supply channeled signals to that block when that block is adjacent
     * B) receive channeled signals to that block when that block is adjacent
     * C) render additional lines of wires attaching to that block.
     * <p>
     * It does not affect the result of WireBlock::canConnectRedstone.
     * <p>
     * The default behaviour is to always return false from the wireconnector function.
     *
     * @return The cable connectability registry
     */
    public static Map<Block, WireConnector> getCableConnectabilityRegistry() {
        return APIRegistries.getCableConnectabilities();
    }

    public static WireConnector getDefaultCableConnector() {
        return DefaultWireProperties.DEFAULT_CABLE_CONNECTOR;
    }
}
