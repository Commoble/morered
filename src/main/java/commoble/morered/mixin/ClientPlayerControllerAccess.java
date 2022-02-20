package commoble.morered.mixin;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.gen.Accessor;
//import org.spongepowered.asm.mixin.gen.Invoker;
//
//import net.minecraft.client.multiplayer.PlayerController;
//
//@Mixin(PlayerController.class)
//public interface ClientPlayerControllerAccess
//{
//	@Accessor
//	public void setDestroyDelay(int ticks);
//
//	@Accessor
//	public BlockPos getDestroyBlockPos();
//
//	@Accessor
//	public void setIsDestroying(boolean is);
//
//	@Accessor
//	public void setDestroyProgress(float damage);
//
//	@Accessor
//	public void setDestroyTicks(float count);
//
//	@Invoker
//	public boolean callSameDestroyTarget(BlockPos pos);
//
//	@Invoker
//	public void callSendBlockAction(ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction dir);
//}
