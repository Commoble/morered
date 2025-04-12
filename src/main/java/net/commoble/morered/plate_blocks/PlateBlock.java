package net.commoble.morered.plate_blocks;

import net.commoble.morered.TwentyFourBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

public class PlateBlock extends TwentyFourBlock
{
	public static final EnumProperty<Direction> ATTACHMENT_DIRECTION = TwentyFourBlock.ATTACHMENT_DIRECTION;
	public static final IntegerProperty ROTATION = TwentyFourBlock.ROTATION;

	public static final VoxelShape[] SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 14, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 2),
		Block.box(0, 0, 14, 16, 16, 16), Block.box(0, 0, 0, 2, 16, 16), Block.box(14, 0, 0, 16, 16, 16) };
	

	public PlateBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		if (state.hasProperty(ATTACHMENT_DIRECTION))
		{
			return SHAPES_BY_DIRECTION[state.getValue(ATTACHMENT_DIRECTION).ordinal()];
		}
		else
		{
			return SHAPES_BY_DIRECTION[0];
		}
	}
	
	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		boolean isPlayerHoldingWrench = stack.is(Tags.Items.TOOLS_WRENCH);
		
		// rotate the block when the player pokes it with a wrench
		if (isPlayerHoldingWrench && !level.isClientSide)
		{
			level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS,
				0.1F + level.random.nextFloat()*0.1F,
				0.7F + level.random.nextFloat()*0.1F);
			int newRotation = (state.getValue(ROTATION) + 1) % 4;
			BlockState newState = state.setValue(ROTATION, newRotation);
			level.setBlockAndUpdate(pos, newState);
		}
		
		return isPlayerHoldingWrench ? InteractionResult.SUCCESS : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}
}
