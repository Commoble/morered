package commoble.morered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import commoble.morered.client.ClientMixinCallbacks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;

@Mixin(BlockItem.class)
public class ClientBlockItemMixin extends Item
{	
	public ClientBlockItemMixin(Item.Properties properties)
	{
		super(properties);
	}

	@Inject(method="useOn", at=@At("HEAD"), cancellable = true)
	public void whenUseOn(ItemUseContext context, CallbackInfoReturnable<ActionResultType> info)
	{
			ClientMixinCallbacks.onBlockItemUseOn(context, info);
	}
}