package commoble.morered.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import commoble.morered.client.ClientMixinCallbacks;

@Mixin(BlockItem.class)
public class ClientBlockItemMixin extends Item {
    public ClientBlockItemMixin(Item.Properties properties) {
        super(properties);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    public void whenUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> info) {
        ClientMixinCallbacks.onBlockItemUseOn(context, info);
    }
}