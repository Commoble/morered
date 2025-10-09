package net.commoble.morered.transportation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.commoble.morered.util.BlockSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class PliersItem extends Item
{	
	public PliersItem(Properties properties)
	{
		super(properties);
	}

	/**
	 * Called when this item is used when targetting a Block
	 */
	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (world.getBlockEntity(pos) instanceof TubeBlockEntity tube)
		{
			return this.onUseOnTube(world, pos, tube, context.getItemInHand(), context.getPlayer(), context.getClickedFace());
		}
		return super.useOn(context);
	}
	
	private InteractionResult onUseOnTube(Level level, BlockPos pos, @Nonnull TubeBlockEntity tube, ItemStack stack, Player player, Direction activatedSide)
	{
		if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel)
		{
			@Nullable BlockSide plieredTube = getPlieredTube(stack);
			BlockState state = level.getBlockState(pos);
			
			// no existing position stored in item
			if (plieredTube == null)
			{
				// if we clicked an unused side of a tube block
				if (state.getBlock() instanceof TubeBlock tubeBlock && !tubeBlock.hasConnectionOnSide(state, activatedSide))
				{
					setPlieredTube(stack, new BlockSide(pos, activatedSide));
				}
				
			}
			else // existing position stored in stack
			{
				BlockPos lastPos = plieredTube.pos();
				Direction lastSide = plieredTube.direction();
				// if player clicked the same tube twice, clear the last-used-position
				if (lastPos.equals(pos))
				{
					removePlieredTube(stack);
				}
				// if tube was already connected to the other position, remove connections
				else if (tube.hasRemoteConnection(lastPos))
				{
					TubeBlockEntity.removeConnection(level, pos, lastPos);
					removePlieredTube(stack);
				}
				else // we clicked a different tube that doesn't have an existing connection to the original tube
				{
					// if the tube is already connected on this side, cancel the connection
					if (tube.hasRemoteConnection(activatedSide))
					{
						removePlieredTube(stack);
						PacketDistributor.sendToPlayer(serverPlayer, new TubeBreakPacket(Vec3.atCenterOf(lastPos), Vec3.atCenterOf(pos)));
						serverPlayer.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
						return InteractionResult.SUCCESS;
					}
					// do a raytrace to check for interruptions
					Vec3 startVec = RaytraceHelper.getTubeSideCenter(lastPos, lastSide);
					Vec3 endVec = RaytraceHelper.getTubeSideCenter(pos, activatedSide);
					Vec3 hit = RaytraceHelper.getTubeRaytraceHit(startVec, endVec, level);
					BlockState lastState = level.getBlockState(lastPos);
					
					// if tube wasn't connected but they can't be connected due to a block in the way, interrupt the connection
					if (hit != null)
					{
						removePlieredTube(stack);
						PacketDistributor.sendToPlayer(serverPlayer, new TubeBreakPacket(startVec, endVec));
						serverLevel.sendParticles(serverPlayer, DustParticleOptions.REDSTONE, false, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
						serverPlayer.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
					}
					// if we clicked the same side of two different tubes, deny the connection attempt (fixes an edge case)
					else if (activatedSide == lastSide)
					{
						removePlieredTube(stack);
						PacketDistributor.sendToPlayer(serverPlayer, new TubeBreakPacket(startVec, endVec));
						serverLevel.sendParticles(serverPlayer, DustParticleOptions.REDSTONE, false, false, endVec.x, endVec.y, endVec.z, 5, .05, .05, .05, 0);
						serverPlayer.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
					}
					else if (state.getBlock() instanceof TubeBlock tubeBlock && !tubeBlock.hasConnectionOnSide(state, activatedSide))
					{
						// if tube wasn't connected to the first tube or another tube, connect them if they're close enough
						if (pos.closerThan(lastPos, MoreRed.SERVERCONFIG.maxTubeConnectionRange().get())
							&& lastState.getBlock() instanceof TubeBlock lastTubeBlock && !lastTubeBlock.hasConnectionOnSide(lastState, lastSide))
						{
							
							removePlieredTube(stack);
							if (level.getBlockEntity(lastPos) instanceof TubeBlockEntity lastPost)
							{
								RemoteConnection originalConnection = lastPost.getRemoteConnection(lastSide);
								
								// if the original tube was already connected on the given side, make sure to remove the original connection first
								if (originalConnection != null)
								{
									TubeBlockEntity.removeConnection(level, lastPos, originalConnection.toPos);
								}
								
								TubeBlockEntity.addConnection(level, lastPost, lastSide, tube, activatedSide);
							}
							stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
							
						}
						else // too far away, initiate a new connection from here
						{
							setPlieredTube(stack, new BlockSide(pos, activatedSide));
							// TODO give feedback to player
						}
					}
				}
			}
			level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS,
				0.1F + level.random.nextFloat()*0.1F,
				0.7F + level.random.nextFloat()*0.1F);
		}
		
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot)
	{
		super.inventoryTick(stack, level, entity, slot);
		if (!level.isClientSide())
		{
			@Nullable BlockSide plieredTube = getPlieredTube(stack);
			if (plieredTube != null)
			{
				BlockPos lastTubePos = plieredTube.pos();
				if (shouldRemoveConnection(lastTubePos, level, entity))
				{
					breakPendingConnection(stack,lastTubePos,entity,level);
				}
			}
		}
	}
	
	public static boolean shouldRemoveConnection(BlockPos connectionPos, Level level, Entity holder)
	{
		double maxDistance = MoreRed.SERVERCONFIG.maxTubeConnectionRange().get();
		if (holder.position().distanceToSqr(Vec3.atCenterOf(connectionPos)) > maxDistance*maxDistance)
		{
			// too far away, remove connection
			return true;
		}
		 // if blockentity doesn't exist or isn't connectable, remove connection
		return !(level.getBlockEntity(connectionPos) instanceof TubeBlockEntity);
	}
	
	public static void breakPendingConnection(ItemStack stack, BlockPos connectingPos, Entity holder, Level level)
	{
		removePlieredTube(stack);
		if (holder instanceof ServerPlayer serverPlayer)
		{
			PacketDistributor.sendToPlayer(serverPlayer,
				new TubeBreakPacket(
					Vec3.atCenterOf(connectingPos),
					new Vec3(holder.getX(), holder.getEyeY(), holder.getZ())));
		}
	}
	
	public static void setPlieredTube(ItemStack stack, BlockSide blockSide)
	{
		stack.set(MoreRed.PLIERED_TUBE_DATA_COMPONENT.get(), blockSide);
	}
	
	public static @Nullable BlockSide getPlieredTube(ItemStack stack)
	{
		return stack.get(MoreRed.PLIERED_TUBE_DATA_COMPONENT.get());
	}
	
	public static void removePlieredTube(ItemStack stack)
	{
		stack.remove(MoreRed.PLIERED_TUBE_DATA_COMPONENT.get());
	}
}
