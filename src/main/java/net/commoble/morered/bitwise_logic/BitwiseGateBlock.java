package net.commoble.morered.bitwise_logic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.Receiver;
import net.commoble.exmachina.api.SignalGraphUpdateGameEvent;
import net.commoble.morered.CommonTags;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.PlateBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.DirectionHelper;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BitwiseGateBlock extends PlateBlock implements EntityBlock
{
	public static final VoxelShape[] DOUBLE_PLATE_SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.box(0, 0, 0, 16, 4, 16),
		Block.box(0, 12, 0, 16, 16, 16),
		Block.box(0, 0, 0, 16, 16, 4),
		Block.box(0, 0, 12, 16, 16, 16),
		Block.box(0, 0, 0, 4, 16, 16),
		Block.box(12, 0, 0, 16, 16, 16) };
	
	public abstract @Nullable Receiver getReceiverEndpoint(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Direction receiverSide, Face connectedFace, Channel channel);
	public abstract Collection<Receiver> getAllReceivers(BlockGetter level, BlockPos receiverPos, BlockState receiverState, Channel channel);
	protected abstract List<Direction> getInputDirections(BlockState thisState);
	
	protected LogicFunction operator;
	
	public BitwiseGateBlock(Properties properties, LogicFunction operator)
	{
		super(properties);
		this.operator = operator;
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		if (state.hasProperty(ATTACHMENT_DIRECTION))
		{
			return DOUBLE_PLATE_SHAPES_BY_DIRECTION[state.getValue(ATTACHMENT_DIRECTION).ordinal()];
		}
		else
		{
			return DOUBLE_PLATE_SHAPES_BY_DIRECTION[0];
		}
	}
	
	// called when setBlock puts this block in the world or changes its state
	// direction-state-dependent capabilities need to be invalidated here
	@Override
	protected void onPlace(BlockState newState, Level level, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		super.onPlace(newState, level, pos, oldState, isMoving);
		for (Direction dir : this.getInputDirections(newState))
		{
			SignalGraphUpdateGameEvent.scheduleSignalGraphUpdate(level, pos.relative(dir));
		}
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(newState);
		SignalGraphUpdateGameEvent.scheduleSignalGraphUpdate(level, pos);
		SignalGraphUpdateGameEvent.scheduleSignalGraphUpdate(level, pos.relative(primaryOutputDirection));
	}

	@Override
	protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving)
	{
		super.onRemove(oldState, level, pos, newState, isMoving);
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(newState);
		SignalGraphUpdateGameEvent.scheduleSignalGraphUpdate(level, pos.relative(primaryOutputDirection));
	}
	
	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		boolean isPlayerHoldingWrench = stack.is(CommonTags.Items.WRENCHES);
		
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

	public Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints(BlockGetter level, BlockPos supplierPos, BlockState supplierState, Direction supplierSide,
		Face connectedFace)
	{
		Direction attachmentDir = supplierState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		if (supplierSide != attachmentDir || connectedFace.attachmentSide() != attachmentDir || !(level.getBlockEntity(supplierPos) instanceof BitwiseGateBlockEntity gate))
		{
			return Map.of();
		}

		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(supplierState);
		BlockPos wirePos = connectedFace.pos();
		@Nullable Direction directionToNeighbor = DirectionHelper.getDirectionToNeighborPos(supplierPos, wirePos);
		if (directionToNeighbor != primaryOutputDirection)
		{
			return Map.of();
		}
		
		return gate.getSupplierEndpoints();
	}
	
}
