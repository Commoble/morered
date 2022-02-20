package commoble.morered;

import commoble.morered.gatecrafting_plinth.GatecraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ContainerRegistrar {
    public static final DeferredRegister<MenuType<?>> CONTAINER_TYPES =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, MoreRed.MODID);

    public static final RegistryObject<MenuType<GatecraftingContainer>> GATECRAFTING =
            CONTAINER_TYPES.register(ObjectNames.GATECRAFTING_PLINTH,
                    () -> IForgeMenuType.create(GatecraftingContainer::getClientContainer));
}
