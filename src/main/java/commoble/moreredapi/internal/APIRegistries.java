package commoble.moreredapi.internal;

import com.google.common.collect.ImmutableMap;
import commoble.moreredapi.ExpandedPowerSupplier;
import commoble.moreredapi.WireConnector;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class APIRegistries {
    private static Map<Block, WireConnector> wireConnectabilities = new ConcurrentHashMap<>();

    public static Map<Block, WireConnector> getWireConnectabilities() {
        return wireConnectabilities;
    }

    private static Map<Block, ExpandedPowerSupplier> expandedPowerSuppliers = new ConcurrentHashMap<>();

    public static Map<Block, ExpandedPowerSupplier> getExpandedPowerSuppliers() {
        return expandedPowerSuppliers;
    }

    private static Map<Block, WireConnector> cableConnectabilities = new ConcurrentHashMap<>();

    public static Map<Block, WireConnector> getCableConnectabilities() {
        return cableConnectabilities;
    }

    public static void freezeRegistries() {
        wireConnectabilities = freezeAPIRegistry(wireConnectabilities);
        expandedPowerSuppliers = freezeAPIRegistry(expandedPowerSuppliers);
        cableConnectabilities = freezeAPIRegistry(cableConnectabilities);
    }

    private static <K, V> Map<K, V> freezeAPIRegistry(Map<K, V> mutableRegistry) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        mutableRegistry.forEach(builder::put);
        return builder.build();
    }
}
