package commoble.morered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

@Mixin(PlayerController.class)
public interface ClientPlayerControllerAccess
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
	
	@Invoker
	public void callSendBlockAction(CPlayerDiggingPacket.Action action, BlockPos pos, Direction dir);
}
