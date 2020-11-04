package commoble.morered.wire_post;

import java.util.Optional;

import javax.annotation.Nonnull;

import commoble.morered.MoreRed;
import commoble.morered.ServerConfig;
import commoble.morered.TileEntityRegistrar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import net.minecraft.item.Item.Properties;

public class WireSpoolItem extends Item
{
	public static final String LAST_POST_POS = "last_post_pos";
	
	public WireSpoolItem(Properties properties)
	{
		super(properties);
	}

	/**
	 * Called when this item is used when targetting a Block
	 */
	@Override
	public ActionResultType onItemUse(ItemUseContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getPos();
		return WirePostTileEntity.getPost(world, pos)
			.map(post -> this.onUseOnPost(world, pos, post, context.getItem(), context.getPlayer()))
			.orElseGet(() -> super.onItemUse(context));
	}
	
	private ActionResultType onUseOnPost(World world, BlockPos pos, @Nonnull WirePostTileEntity post, ItemStack stack, PlayerEntity player)
	{
		if (!world.isRemote)
		{
			CompoundNBT nbt = stack.getChildTag(LAST_POST_POS);
			
			if (nbt == null)
			{
				stack.setTagInfo(LAST_POST_POS, NBTUtil.writeBlockPos(pos));
			}
			else // existing position stored in stack
			{
				BlockPos lastPos = NBTUtil.readBlockPos(nbt);
				// if player clicked the same post twice, clear the last-used-position
				if (lastPos.equals(pos))
				{
					stack.removeChildTag(LAST_POST_POS);
				}
				// if post was already connected to the other position, remove connections
				else if (post.hasRemoteConnection(lastPos))
				{
					WirePostTileEntity.removeConnection(world, pos, lastPos);
					stack.removeChildTag(LAST_POST_POS);
				}
				else // we clicked a different post that doesn't have an existing connection to the original post
				{
					// do a curved raytrace to check for interruptions
					boolean lastPosIsHigher = pos.getY() < lastPos.getY();
					BlockPos upperPos = lastPosIsHigher ? lastPos : pos;
					BlockPos lowerPos = lastPosIsHigher ? pos : lastPos; 
					Vector3d hit = SlackInterpolator.getWireRaytraceHit(lowerPos, upperPos, world);
					
					// if post wasn't connected but they can't be connected due to a block in the way, interrupt the connection
					if (hit != null)
					{
						stack.removeChildTag(LAST_POST_POS);
						if (player instanceof ServerPlayerEntity && world instanceof ServerWorld)
						{
							MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), new WireBreakPacket(WirePostTileEntity.getConnectionVector(lowerPos), WirePostTileEntity.getConnectionVector(upperPos)));
							((ServerWorld)world).spawnParticle((ServerPlayerEntity)player, RedstoneParticleData.REDSTONE_DUST, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
							
							((ServerPlayerEntity)player).playSound(SoundEvents.ENTITY_WANDERING_TRADER_HURT, SoundCategory.BLOCKS, 0.5F, 2F);
						}
					}
					// if post wasn't connected, connect them if they're close enough
					else if (pos.withinDistance(lastPos, ServerConfig.INSTANCE.max_wire_post_connection_range.get()))
					{
						stack.removeChildTag(LAST_POST_POS);
						WirePostTileEntity.getPost(world, lastPos)
							.ifPresent(lastPost -> WirePostTileEntity.addConnection(world, post, lastPost));
						stack.damageItem(1, player, thePlayer -> thePlayer.sendBreakAnimation(EquipmentSlotType.MAINHAND));
							
					}
					else	// too far away, initiate a new connection from here
					{
						stack.setTagInfo(LAST_POST_POS, NBTUtil.writeBlockPos(pos));
						// TODO give feedback to player
					}
				}
			}
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
				0.2F + world.rand.nextFloat()*0.1F,
				0.7F + world.rand.nextFloat()*0.1F);
		}
		
		return ActionResultType.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
		if (!worldIn.isRemote)
		{
			Optional.ofNullable(stack.getChildTag(LAST_POST_POS))
				.map(nbt -> NBTUtil.readBlockPos(nbt))
				.filter(pos -> shouldRemoveConnection(pos, worldIn, entityIn))
				.ifPresent(pos -> breakPendingConnection(stack, pos, entityIn, worldIn));
		}
	}
	
	public static boolean shouldRemoveConnection(BlockPos connectionPos, World world, Entity holder)
	{
		double maxDistance = ServerConfig.INSTANCE.max_wire_post_connection_range.get();
		if (holder.getPositionVec().squareDistanceTo(Vector3d.copyCentered(connectionPos)) > maxDistance*maxDistance)
		{
			return true;
		}
		TileEntity te = world.getTileEntity(connectionPos);
		return te == null || te.getType() != TileEntityRegistrar.REDWIRE_POST.get();
	}
	
	public static void breakPendingConnection(ItemStack stack, BlockPos connectingPos, Entity holder, World world)
	{
		stack.removeChildTag(LAST_POST_POS);
		if (holder instanceof ServerPlayerEntity)
		{
			MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)holder),
				new WireBreakPacket(
					WirePostTileEntity.getConnectionVector(connectingPos),
					new Vector3d(holder.getPosX(), holder.getPosYEye(), holder.getPosZ())));
		}
	}
}
