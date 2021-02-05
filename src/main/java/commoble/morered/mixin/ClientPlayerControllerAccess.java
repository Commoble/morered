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
	public void setBlockHitDelay(int ticks);
	
	@Accessor
	public BlockPos getCurrentBlock();
	
	@Accessor
	public void setIsHittingBlock(boolean is);
	
	@Accessor
	public void setCurBlockDamageMP(float damage);
	
	@Accessor
	public void setStepSoundTickCounter(float count);
	
	@Invoker
	public boolean callIsHittingPosition(BlockPos pos);
	
	@Invoker
	public void callSendDiggingPacket(CPlayerDiggingPacket.Action action, BlockPos pos, Direction dir);
}
