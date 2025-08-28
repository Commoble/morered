package net.commoble.morered.mechanisms;

import java.util.Map;

import javax.annotation.Nullable;

import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
		Level level = context.getLevel();
		Direction activatedFace = context.getClickedFace();
		// if the block would be replaceable, just replace it (use default behavior)
		// otherwise, see if we're adding the block to an existing gearblock
		// if placePos == activatedPos, we're replacing a block
		// so check if they're *not* equal
		if (!placePos.equals(activatedPos))
		{
			BlockState existingPlacePosState = level.getBlockState(placePos);
			Direction attachmentSide = activatedFace.getOpposite();
			BooleanProperty sideProperty = FaceSegmentBlock.getProperty(attachmentSide);
			Block placePosBlock = existingPlacePosState.getBlock();
			
			// firstly: if we used this on a gear/gears block, we might want to add the new gear inside that block
			BlockState activatedState = level.getBlockState(activatedPos);
			if (activatedState.getBlock() == MoreRed.GEARS_BLOCK.get()
				&& activatedState.getValue(sideProperty))
			{
				// use a finagled context to make sure the block is placed at the activation pos
				Vec3 relativeHitVec = context.getClickLocation().subtract(Vec3.atLowerCornerOf(activatedPos));
				Direction newSideToAdd = BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec, attachmentSide);
				BooleanProperty newSideProperty = FaceSegmentBlock.getProperty(newSideToAdd);
				if (!activatedState.getValue(newSideProperty))
				{
					return upgradeGears(level, activatedPos, activatedState, newSideProperty, GearUpgradeContext.at(blockItemContext, activatedPos, activatedFace));	
				}
			}
			if (activatedState.getBlock() instanceof GearBlock)
			{
				// if we clicked the inside of a gear block, add a new gear to that block
				Direction existingSide = activatedState.getValue(GearBlock.FACING);
				if (attachmentSide == existingSide)
				{
					// use a finagled context to make sure the block is placed at the activation pos
					Vec3 relativeHitVec = context.getClickLocation().subtract(Vec3.atLowerCornerOf(activatedPos));
					Direction newSideToAdd = BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec, attachmentSide);
					return upgradeGear(level, activatedPos, activatedState, existingSide, newSideToAdd, GearUpgradeContext.at(blockItemContext, activatedPos, activatedFace));	
				}
			}
			// if the position of placement contains a gear assembly
			// but we don't have a gear on the given face,
			if (placePosBlock == MoreRed.GEARS_BLOCK.get()
				&& !existingPlacePosState.getValue(sideProperty))
			{
				return upgradeGears(level, placePos, existingPlacePosState, sideProperty, blockItemContext);
			}
			// otherwise, check if we can upgrade a single gear to a gear assembly
			if (placePosBlock instanceof GearBlock)
			{
				Direction existingGearSide = existingPlacePosState.getValue(GearBlock.FACING);
				if (existingGearSide != attachmentSide)
				{
					return upgradeGear(level, placePos, existingPlacePosState, existingGearSide, attachmentSide, blockItemContext);
				}
			}
		}
		
		// otherwise, use it like a regular blockitem
		return super.useOn(context);
	}
	
	private InteractionResult upgradeGears(Level level, BlockPos pos, BlockState existingGearsState, BooleanProperty newSideProperty, BlockPlaceContext blockPlaceContext)
	{
		// then add the gear to the block and decrement the itemstack and return
		// (an EntityPlaceBlockEvent is fired by existing forge hooks)
		BlockState newState = existingGearsState.setValue(newSideProperty, true);
		// attempt to set the block in the world with standard flags
		if (!this.placeBlock(blockPlaceContext, newState))
		{
			return InteractionResult.FAIL;
		}
		// we should have parity with some of the standard blockitem stuff
		// but we can skip the bits that deal with NBT and tile entities
		ItemStack stack = blockPlaceContext.getItemInHand();
		@Nullable Player player = blockPlaceContext.getPlayer();
        newState.getBlock().setPlacedBy(level, pos, newState, player, stack);
		if (player instanceof ServerPlayer)
		{
			CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, stack);
		}

		SoundType soundtype = newState.getSoundType(level, pos, player);
		level.playSound(player, pos, this.getPlaceSound(newState, level, pos, player), SoundSource.BLOCKS,
			(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
		if (player == null || !player.getAbilities().instabuild)
		{
			stack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}
	
	private InteractionResult upgradeGear(Level level, BlockPos pos, BlockState existingState, Direction existingGearSide, Direction newAttachmentSide, BlockPlaceContext blockPlaceContext)
	{
		BooleanProperty newSideProperty = FaceSegmentBlock.getProperty(newAttachmentSide);
		// set a new gear assembly using existing gear side + the new side we're placing
		BlockState newState = MoreRed.GEARS_BLOCK.get().defaultBlockState()
			.setValue(FaceSegmentBlock.getProperty(existingGearSide), true)
			.setValue(newSideProperty, true);
			
		// attempt to set the block in the world with standard flags
		if (!this.placeBlock(blockPlaceContext, newState))
		{
			return InteractionResult.FAIL;
		}
		// we should have parity with some of the standard blockitem stuff
		// but we can skip the bits that deal with NBT and tile entities
		ItemStack stack = blockPlaceContext.getItemInHand();
		@Nullable Player player = blockPlaceContext.getPlayer();
		// don't do setPlacedBy, we need to finagle the existing gear into the data
        if (level.getBlockEntity(pos) instanceof GenericBlockEntity be)
        {
        	Map<Direction,ItemStack> items = Map.of(
        		existingGearSide, new ItemStack(existingState.getBlock()),
        		newAttachmentSide, stack.copyWithCount(1));
        	be.set(MoreRed.GEARS_DATA_COMPONENT.get(), items);
        }
		if (player instanceof ServerPlayer)
		{
			CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, stack);
		}

		SoundType soundtype = newState.getSoundType(level, pos, player);
		level.playSound(player, pos, this.getPlaceSound(newState, level, pos, player), SoundSource.BLOCKS,
			(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
		if (player == null || !player.getAbilities().instabuild)
		{
			stack.shrink(1);
		}

		// return SUCCESS for client thread worlds, CONSUME for server thread worlds (same as regular blockitem)
		return InteractionResult.SUCCESS;
	}
	
	private static class GearUpgradeContext extends BlockPlaceContext
	{
		private final BlockPos actualPlacePos;
		public GearUpgradeContext(Player player, InteractionHand hand, ItemStack stack, BlockHitResult result, BlockPos activatedPos)
		{
			super(player, hand, stack, result);
			this.actualPlacePos = activatedPos;
		}
		
		public static GearUpgradeContext at(BlockPlaceContext delegate, BlockPos activatedPos, Direction clickFace)
		{
			return new GearUpgradeContext(
				delegate.getPlayer(),
				delegate.getHand(),
				delegate.getItemInHand(),
				new BlockHitResult(
					new Vec3(
						activatedPos.getX() + 0.5 + clickFace.getStepX() * 0.5,
						activatedPos.getY() + 0.5 + clickFace.getStepY() * 0.5,
						activatedPos.getZ() + 0.5 + clickFace.getStepZ() * 0.5
					),
					clickFace,
					activatedPos,
					false
				),
				activatedPos);
		}
		
		@Override
		public BlockPos getClickedPos()
		{
			return this.actualPlacePos;
		}
		
	}
}
