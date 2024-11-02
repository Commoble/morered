package net.commoble.morered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.players.PlayerList;

@Mixin(PlayerList.class)
public class PlayerListMixin
{
	@Inject(method="reloadResources", at=@At("TAIL"))
	public void afterReloadResources(CallbackInfo info)
	{
		
	}
}
