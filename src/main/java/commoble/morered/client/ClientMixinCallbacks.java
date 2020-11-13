package commoble.morered.client;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import commoble.morered.MoreRed;
import commoble.morered.wire_post.FakeStateWorld;
import commoble.morered.wire_post.PostsInChunk;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WirePostTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.MoreRedBlockItemHelper;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class ClientMixinCallbacks
{
	public static void onBlockItemUse(ItemUseContext itemContext, CallbackInfoReturnable<ActionResultType> info)
	{
		MoreRed.CLIENT_PROXY.ifPresent(proxy -> onClientBlockItemUse(proxy, itemContext, info));
	}
	
	@SuppressWarnings("deprecation")
	private static void onClientBlockItemUse(ClientProxy proxy, ItemUseContext itemContext, CallbackInfoReturnable<ActionResultType> info)
	{
		World world = itemContext.getWorld();
		ItemStack stack = itemContext.getItem();
		Item item = stack.getItem();

		// we need to check world side because physical clients can have server worlds
		if (world.isRemote && item instanceof BlockItem)
		{
			BlockItem blockItem = (BlockItem)item;
			BlockItemUseContext context = new BlockItemUseContext(itemContext);
			if (context.canPlace())	// return early if we can't place it here for consistency with vanilla (prevent getStateForPlacement check)
			{
				return;
			}
			context = blockItem.getBlockItemUseContext(context);	// allow blockItem to transform use context after checking canPlace
			if (context == null) // BlockItem::getBlockItemUseContext is nullable
			{
				return;
			}
			BlockPos pos = context.getPos();
			BlockState placementState = MoreRedBlockItemHelper.getStateForPlacement(blockItem, context);
			
			if (placementState != null)
			{


				Set<ChunkPos> chunkPositions = PostsInChunk.getRelevantChunkPositionsNearPos(pos);

				chunkLoop:
				for (ChunkPos chunkPos : chunkPositions)
				{
					if (world.isBlockLoaded(chunkPos.asBlockPos()))
					{
						Set<BlockPos> postPositions = proxy.getPostsInChunk(chunkPos);

						Set<BlockPos> checkedPostPositions = new HashSet<BlockPos>();
						for (BlockPos postPos : postPositions)
						{
							TileEntity te = world.getTileEntity(postPos);
							if (te instanceof WirePostTileEntity)
							{
								IBlockReader fakeWorld = new FakeStateWorld(world, pos, placementState);
								Vector3d hit = SlackInterpolator.doesBlockStateIntersectAnyWireOfPost(fakeWorld, postPos, pos, placementState, ((WirePostTileEntity)te).getRemoteConnectionBoxes(), checkedPostPositions);
								if (hit != null)
								{
									PlayerEntity player = context.getPlayer();
									if (player != null)
									{
										world.addParticle(RedstoneParticleData.REDSTONE_DUST, hit.x, hit.y, hit.z, 0.05D, 0.05D, 0.05D);
										player.playSound(SoundEvents.ENTITY_WANDERING_TRADER_HURT, SoundCategory.BLOCKS, 0.5F, 2F);
									}
									info.setReturnValue(ActionResultType.SUCCESS);
									break chunkLoop; // "return" here just continues the inner loop
								}
								else
								{
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
