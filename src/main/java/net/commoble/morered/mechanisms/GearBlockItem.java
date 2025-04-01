package net.commoble.morered.mechanisms;

import java.util.Map;

import javax.annotation.Nullable;

import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class GearBlockItem extends BlockItem
{

	public GearBlockItem(Block block, Properties props)
	{
		super(block, props);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		// we want to be able to add gear states to existing blocks
		// if we've used the item on a block
		// whose activated face points to a neighboring wire block
		// then we update the wire state instead of placing a new wire block
		
		// wrap in a block context as it handles some of the calculations for us
		BlockPlaceContext blockItemContext = new BlockPlaceContext(context);
		// placePos is the block adjacent to the block we clicked
		// unless the block we clicked is replaceable, in which case it's the position of the block we clicked
		// either way, it's the block that our block would be placed at if this were a normal blockitem
		BlockPos placePos = blockItemContext.getClickedPos();
		// UseOnContext#getClickedPos, however, is the block we actually clicked
		BlockPos activatedPos = context.getClickedPos();
		Level world = context.getLevel();
		Direction activatedFace = context.getClickedFace();
		// if the block would be replaceable, just replace it (use default behavior)
		// otherwise, see if we're adding the block to an existing gearblock
		// if placePos == activatedPos, we're replacing a block
		// so check if they're *not* equal
		if (!placePos.equals(activatedPos))
		{
			BlockState existingPlacePosState = world.getBlockState(placePos);
			Direction attachmentSide = activatedFace.getOpposite();
			BooleanProperty sideProperty = FaceSegmentBlock.getProperty(attachmentSide);
			Block targetBlock = existingPlacePosState.getBlock();
			// if the position of placement contains a gear assembly
			// but we don't have a gear on the given face,
			if (targetBlock == MoreRed.get().gearsBlock.get()
				&& !existingPlacePosState.getValue(sideProperty))
			{
				// then add the gear to the block and decrement the itemstack and return
				// (an EntityPlaceBlockEvent is fired by existing forge hooks)
				BlockState newState = existingPlacePosState.setValue(sideProperty, true);
				// attempt to set the block in the world with standard flags
				if (!this.placeBlock(blockItemContext, newState))
				{
					return InteractionResult.FAIL;
				}
				// we should have parity with some of the standard blockitem stuff
				// but we can skip the bits that deal with NBT and tile entities
				ItemStack stack = context.getItemInHand();
				@Nullable Player player = context.getPlayer();
                newState.getBlock().setPlacedBy(world, placePos, newState, player, stack);
				if (player instanceof ServerPlayer)
				{
					CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, placePos, stack);
				}

				SoundType soundtype = newState.getSoundType(world, placePos, player);
				world.playSound(player, placePos, this.getPlaceSound(newState, world, placePos, player), SoundSource.BLOCKS,
					(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
				if (player == null || !player.getAbilities().instabuild)
				{
					stack.shrink(1);
				}

				return InteractionResult.SUCCESS;
			}
			// otherwise, check if we can upgrade a single gear to a gear assembly
			else if (targetBlock instanceof GearBlock)
			{
				Direction existingGearSide = existingPlacePosState.getValue(GearBlock.FACING);
				if (existingGearSide != attachmentSide)
				{
					// set a new gear assembly using existing gear side + the new side we're placing
					BlockState newState = MoreRed.get().gearsBlock.get().defaultBlockState()
						.setValue(FaceSegmentBlock.getProperty(existingGearSide), true)
						.setValue(sideProperty, true);
						
					// attempt to set the block in the world with standard flags
					if (!this.placeBlock(blockItemContext, newState))
					{
						return InteractionResult.FAIL;
					}
					// we should have parity with some of the standard blockitem stuff
					// but we can skip the bits that deal with NBT and tile entities
					ItemStack stack = context.getItemInHand();
					@Nullable Player player = context.getPlayer();
					// don't do setPlacedBy, we need to finagle the existing gear into the data
	                if (world.getBlockEntity(placePos) instanceof GenericBlockEntity be)
	                {
	                	Map<Direction,ItemStack> items = Map.of(
	                		existingGearSide, new ItemStack(existingPlacePosState.getBlock()),
	                		attachmentSide, stack.copyWithCount(1));
	                	be.set(MoreRed.get().gearsDataComponent.get(), items);
	                }
					if (player instanceof ServerPlayer)
					{
						CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, placePos, stack);
					}

					SoundType soundtype = newState.getSoundType(world, placePos, player);
					world.playSound(player, placePos, this.getPlaceSound(newState, world, placePos, player), SoundSource.BLOCKS,
						(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
					if (player == null || !player.getAbilities().instabuild)
					{
						stack.shrink(1);
					}

					// return SUCCESS for client thread worlds, CONSUME for server thread worlds (same as regular blockitem)
					return InteractionResult.SUCCESS;
				}
			}
		}
		
		// otherwise, use it like a regular blockitem
		return super.useOn(context);
	}
}
