package net.commoble.morered.wire_post;

import javax.annotation.Nonnull;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class WireSpoolItem extends Item
{
	protected final TagKey<Block> postBlockTag;
	
	public WireSpoolItem(Properties properties, TagKey<Block> postBlockTag)
	{
		super(properties);
		this.postBlockTag = postBlockTag;
	}

	/**
	 * Called when this item is used when targetting a Block
	 */
	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		return level.getBlockState(pos).is(this.postBlockTag) && level.getBlockEntity(pos) instanceof WirePostBlockEntity post
			? this.onUseOnPost(level, pos, post, context.getItemInHand(), context.getPlayer(), context.getHand())
			: super.useOn(context);
	}
	
	private InteractionResult onUseOnPost(Level level, BlockPos pos, @Nonnull WirePostBlockEntity post, ItemStack stack, Player player, InteractionHand hand)
	{
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer)
		{
			var spooledPost = MoreRed.SPOOLED_POST_DATA_COMPONENT.get();
			BlockPos lastPos = stack.get(spooledPost);
			
			// if we don't currently have a pos stored in the spool, store this pos
			if (lastPos == null)
			{
				stack.set(MoreRed.SPOOLED_POST_DATA_COMPONENT.get(), pos);
			}
			else // existing position stored in stack
			{
				// if player clicked the same post twice, clear the last-used-position
				if (lastPos.equals(pos))
				{
					stack.remove(spooledPost);
				}
				// if post was already connected to the other position, remove connections
				else if (post.hasRemoteConnection(lastPos))
				{
					WirePostBlockEntity.removeConnection(level, pos, lastPos);
					stack.remove(spooledPost);
				}
				else // we clicked a different post that doesn't have an existing connection to the original post
				{
					// do a curved raytrace to check for interruptions
					boolean lastPosIsHigher = pos.getY() < lastPos.getY();
					BlockPos upperPos = lastPosIsHigher ? lastPos : pos;
					BlockPos lowerPos = lastPosIsHigher ? pos : lastPos; 
					Vec3 hit = SlackInterpolator.getWireRaytraceHit(lowerPos, upperPos, level);
					
					// if post wasn't connected but they can't be connected due to a block in the way, interrupt the connection
					if (hit != null)
					{
						stack.remove(spooledPost);
						PacketDistributor.sendToPlayer(serverPlayer, new WireBreakPacket(Vec3.atCenterOf(lowerPos), Vec3.atCenterOf(upperPos)));
						serverLevel.sendParticles(serverPlayer, DustParticleOptions.REDSTONE, false, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
						serverLevel.playSound(null, pos, SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
					}
					// if post wasn't connected, connect them if they're close enough
					else if (pos.closerThan(lastPos, MoreRed.SERVERCONFIG.maxWirePostConnectionRange().get()))
					{
						stack.remove(spooledPost);
						if (level.getBlockEntity(lastPos) instanceof WirePostBlockEntity lastPost)
						{
							WirePostBlockEntity.addConnection(level, post, lastPost);
						}
						stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
							
					}
					else	// too far away, initiate a new connection from here
					{
						stack.set(spooledPost, pos);
						// TODO give feedback to player
					}
				}
			}
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS,
				0.2F + level.random.nextFloat()*0.1F,
				0.7F + level.random.nextFloat()*0.1F);
		}
		
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel worldIn, Entity entityIn, EquipmentSlot slot)
	{
		super.inventoryTick(stack, worldIn, entityIn, slot);
		if (entityIn instanceof ServerPlayer serverPlayer)
		{
			var spooledPost = MoreRed.SPOOLED_POST_DATA_COMPONENT.get();
			BlockPos postPos = stack.get(spooledPost);
			if (postPos != null && this.shouldRemoveConnection(postPos, worldIn, entityIn))
			{
				stack.remove(spooledPost);
				PacketDistributor.sendToPlayer(serverPlayer, new WireBreakPacket(
					Vec3.atCenterOf(postPos),
					entityIn.getEyePosition()));
			}
		}
	}
	
	public boolean shouldRemoveConnection(BlockPos connectionPos, Level world, Entity holder)
	{
		double maxDistance = MoreRed.SERVERCONFIG.maxWirePostConnectionRange().get();
		if (holder.position().distanceToSqr(Vec3.atCenterOf(connectionPos)) > maxDistance*maxDistance)
		{
			return true;
		}
		return !world.getBlockState(connectionPos).is(this.postBlockTag);
	}
}
