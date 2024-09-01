package net.commoble.morered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccess
{
	@Accessor
	public void setDestroyDelay(int ticks);
	
	@Accessor
	public BlockPos getDestroyBlockPos();
	
	@Accessor
	public void setIsDestroying(boolean is);
	
	@Accessor
	public void setDestroyProgress(float damage);
	
	@Accessor
	public void setDestroyTicks(float count);
	
	@Invoker
	public boolean callSameDestroyTarget(BlockPos pos);
}
