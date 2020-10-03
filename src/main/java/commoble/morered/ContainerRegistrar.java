package commoble.morered;

import commoble.morered.gatecrafting_plinth.GatecraftingContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ContainerRegistrar
{
	public static final DeferredRegister<ContainerType<?>> CONTAINER_TYPES = new DeferredRegister<>(ForgeRegistries.CONTAINERS, MoreRed.MODID);
	
	public static final RegistryObject<ContainerType<GatecraftingContainer>> GATECRAFTING = CONTAINER_TYPES.register(ObjectNames.GATECRAFTING_PLINTH,
		() -> new ContainerType<>(GatecraftingContainer::getClientContainer));
}
