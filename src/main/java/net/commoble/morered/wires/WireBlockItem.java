package net.commoble.morered.wires;

import javax.annotation.Nullable;

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

public class WireBlockItem extends BlockItem
{

	public WireBlockItem(Block blockIn, Properties builder)
	{
		super(blockIn, builder);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		// we want to be able to add wire states to existing wire blocks
		// if we've used the item on a solid block
		// whose activated face points to a neighboring wire block
		// then we update the wire state instead of placing a new wire block
		
		// wrap in a BIUC as it handles some of the calculations for us
		BlockPlaceContext blockItemContext = new BlockPlaceContext(context);
		// placePos is the block adjacent to the block we clicked
		// unless the block we clicked is replaceable, in which case it's the position of the block we clicked
		BlockPos placePos = blockItemContext.getClickedPos();
		// we're primarily interested in having clicked a solid face
		BlockPos activatedPos = context.getClickedPos();
		Level world = context.getLevel();
		BlockState activatedState = world.getBlockState(activatedPos);
		Direction activatedFace = context.getClickedFace();
		if (activatedState.isFaceSturdy(world, activatedPos, activatedFace))
		{
			BlockState existingPlacePosState = world.getBlockState(placePos);
			Direction attachmentSide = activatedFace.getOpposite();
			BooleanProperty sideProperty = AbstractWireBlock.INTERIOR_FACES[attachmentSide.ordinal()];
			// if the position of placement contains the same wire type this blockitem places,
			// but we don't have a wire on the given face,
			if (existingPlacePosState.getBlock() == this.getBlock()
				&& existingPlacePosState.hasProperty(sideProperty)
				&& !existingPlacePosState.getValue(sideProperty))
			{
				// then add the wire to the block and decrement the itemstack and return
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

				// return SUCCESS for client thread worlds, CONSUME for server thread worlds (same as regular blockitem)
				return InteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		
		// otherwise, use it like a regular blockitem
		return super.useOn(context);
	}

	
}
