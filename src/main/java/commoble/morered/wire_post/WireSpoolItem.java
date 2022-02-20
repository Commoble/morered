package commoble.morered.wire_post;

import java.util.Optional;

import javax.annotation.Nonnull;

import commoble.morered.MoreRed;
import commoble.morered.foundation.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.Tag;
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
import net.minecraftforge.network.PacketDistributor;

public class WireSpoolItem extends Item {
    public static final String LAST_POST_POS = "last_post_pos";

    protected final Tag<Block> postBlockTag;

    public WireSpoolItem(Properties properties, Tag<Block> postBlockTag) {
        super(properties);
        this.postBlockTag = postBlockTag;
    }

    /**
     * Called when this item is used when targetting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return world.getBlockState(pos).is(this.postBlockTag)
                ? WirePostTileEntity.getPost(world, pos)
                .map(post -> this.onUseOnPost(world, pos, post, context.getItemInHand(), context.getPlayer()))
                .orElseGet(() -> super.useOn(context))
                : super.useOn(context);
    }

    private InteractionResult onUseOnPost(Level world, BlockPos pos, @Nonnull WirePostTileEntity post,
                                          ItemStack stack, Player player) {
        if (!world.isClientSide) {
            CompoundTag nbt = stack.getTagElement(LAST_POST_POS);

            if (nbt == null) {
                stack.addTagElement(LAST_POST_POS, NbtUtils.writeBlockPos(pos));
            } else // existing position stored in stack
            {
                BlockPos lastPos = NbtUtils.readBlockPos(nbt);
                // if player clicked the same post twice, clear the last-used-position
                if (lastPos.equals(pos)) {
                    stack.removeTagKey(LAST_POST_POS);
                }
                // if post was already connected to the other position, remove connections
                else if (post.hasRemoteConnection(lastPos)) {
                    WirePostTileEntity.removeConnection(world, pos, lastPos);
                    stack.removeTagKey(LAST_POST_POS);
                } else // we clicked a different post that doesn't have an existing connection to the original post
                {
                    // do a curved raytrace to check for interruptions
                    boolean lastPosIsHigher = pos.getY() < lastPos.getY();
                    BlockPos upperPos = lastPosIsHigher ? lastPos : pos;
                    BlockPos lowerPos = lastPosIsHigher ? pos : lastPos;
                    Vec3 hit = SlackInterpolator.getWireRaytraceHit(lowerPos, upperPos, world);

                    // if post wasn't connected but they can't be connected due to a block in the way, interrupt the
                    // connection
                    if (hit != null) {
                        stack.removeTagKey(LAST_POST_POS);
                        if (player instanceof ServerPlayer && world instanceof ServerLevel) {
                            MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                    new WireBreakPacket(WirePostTileEntity.getConnectionVector(lowerPos),
                                            WirePostTileEntity.getConnectionVector(upperPos)));
                            ((ServerLevel) world).sendParticles((ServerPlayer) player, DustParticleOptions.REDSTONE,
                                    false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);

                            player.playNotifySound(SoundEvents.WANDERING_TRADER_HURT,
                                    SoundSource.BLOCKS, 0.5F, 2F);
                        }
                    }
                    // if post wasn't connected, connect them if they're close enough
                    else if (pos.closerThan(lastPos, AllConfigs.SERVER.maxWirePostConnectionRange.getD())) {
                        stack.removeTagKey(LAST_POST_POS);
                        WirePostTileEntity.getPost(world, lastPos)
                                .ifPresent(lastPost -> WirePostTileEntity.addConnection(world, post, lastPost));
                        stack.hurtAndBreak(1, player,
                                thePlayer -> thePlayer.broadcastBreakEvent(EquipmentSlot.MAINHAND));

                    } else    // too far away, initiate a new connection from here
                    {
                        stack.addTagElement(LAST_POST_POS, NbtUtils.writeBlockPos(pos));
                        // TODO give feedback to player
                    }
                }
            }
            world.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS,
                    0.2F + world.random.nextFloat() * 0.1F,
                    0.7F + world.random.nextFloat() * 0.1F);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (!worldIn.isClientSide) {
            Optional.ofNullable(stack.getTagElement(LAST_POST_POS))
                    .map(NbtUtils::readBlockPos)
                    .filter(pos -> this.shouldRemoveConnection(pos, worldIn, entityIn))
                    .ifPresent(pos -> breakPendingConnection(stack, pos, entityIn, worldIn));
        }
    }

    public boolean shouldRemoveConnection(BlockPos connectionPos, Level world, Entity holder) {
        double maxDistance = AllConfigs.SERVER.maxWirePostConnectionRange.getD();
        if (holder.position().distanceToSqr(Vec3.atCenterOf(connectionPos)) > maxDistance * maxDistance) {
            return true;
        }
        return !world.getBlockState(connectionPos).is(this.postBlockTag);
    }

    public static void breakPendingConnection(ItemStack stack, BlockPos connectingPos, Entity holder, Level world) {
        stack.removeTagKey(LAST_POST_POS);
        if (holder instanceof ServerPlayer) {
            MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) holder),
                    new WireBreakPacket(
                            WirePostTileEntity.getConnectionVector(connectingPos),
                            new Vec3(holder.getX(), holder.getEyeY(), holder.getZ())));
        }
    }
}
