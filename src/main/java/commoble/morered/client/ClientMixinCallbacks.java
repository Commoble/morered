package commoble.morered.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import commoble.morered.MoreRed;
import commoble.morered.wire_post.FakeStateWorld;
import commoble.morered.wire_post.PostsInChunk;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WirePostTileEntity;

public class ClientMixinCallbacks {
    public static void onBlockItemUseOn(UseOnContext itemContext, CallbackInfoReturnable<InteractionResult> info) {
        MoreRed.CLIENT_PROXY.ifPresent(proxy -> onClientBlockItemUse(proxy, itemContext, info));
    }

    @SuppressWarnings("deprecation")
    private static void onClientBlockItemUse(ClientProxy proxy, UseOnContext itemContext,
                                             CallbackInfoReturnable<InteractionResult> info) {
        Level world = itemContext.getLevel();
        ItemStack stack = itemContext.getItemInHand();
        Item item = stack.getItem();

        // we need to check world side because physical clients can have server worlds
        if (world.isClientSide && item instanceof BlockItem blockItem) {
            BlockPlaceContext context = new BlockPlaceContext(itemContext);
            if (!context.canPlace())    // return early if we can't place it here for consistency with vanilla
            // (prevent getStateForPlacement check)
            {
                return;
            }
            context = blockItem.updatePlacementContext(context);    // allow blockItem to transform use context after
            // checking canPlace
            if (context == null) // BlockItem::getBlockItemUseContext is nullable
            {
                return;
            }
            BlockPos pos = context.getClickedPos();
            BlockState placementState = ((BlockItem) item).getBlock().getStateForPlacement(context);

            if (placementState != null) {


                Set<ChunkPos> chunkPositions = PostsInChunk.getRelevantChunkPositionsNearPos(pos);

                chunkLoop:
                for (ChunkPos chunkPos : chunkPositions) {
                    if (world.hasChunkAt(chunkPos.getWorldPosition())) {
                        Set<BlockPos> postPositions = proxy.getPostsInChunk(chunkPos);

                        Set<BlockPos> checkedPostPositions = new HashSet<BlockPos>();
                        for (BlockPos postPos : postPositions) {
                            BlockEntity te = world.getBlockEntity(postPos);
                            if (te instanceof WirePostTileEntity) {
                                BlockGetter fakeWorld = new FakeStateWorld(world, pos, placementState);
                                Vec3 hit = SlackInterpolator.doesBlockStateIntersectAnyWireOfPost(fakeWorld, postPos,
                                        pos, placementState, ((WirePostTileEntity) te).getRemoteConnectionBoxes(),
                                        checkedPostPositions);
                                if (hit != null) {
                                    Player player = context.getPlayer();
                                    if (player != null) {
                                        world.addParticle(DustParticleOptions.REDSTONE, hit.x, hit.y, hit.z, 0.05D, 0.05D, 0.05D);
                                        player.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
                                    }
                                    info.setReturnValue(InteractionResult.SUCCESS);
                                    break chunkLoop; // "return" here just continues the inner loop
                                } else {
                                    checkedPostPositions.add(postPos);
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
