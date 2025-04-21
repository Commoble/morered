package net.commoble.morered.transportation;

import java.util.HashMap;
import java.util.Map;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.PlayerData;
import net.commoble.morered.TwentyFourBlock;
import net.commoble.morered.util.BlockStateUtil;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;

public class ExtractorBlock extends TwentyFourBlock implements EntityBlock
{

	public ExtractorBlock(Properties properties)
	{
		super(properties);
	}
	
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		boolean isPlayerHoldingWrench = stack.is(Tags.Items.TOOLS_WRENCH);
		
		// rotate the block when the player pokes it with a wrench
		if (isPlayerHoldingWrench && !level.isClientSide)
		{
			BlockState newState;
			level.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS,
				0.9F + level.random.nextFloat()*0.1F,
				0.95F + level.random.nextFloat()*0.1F);
			if (PlayerData.getSprinting(player.getUUID()))
			{
				// rotate around small gear... weird math here
				// firstly, figure out which way small gear is facing
				Direction bigDir = state.getValue(ATTACHMENT_DIRECTION);
				int rotation = state.getValue(ROTATION);
				Direction smallDir = BlockStateUtil.getOutputDirection(bigDir, rotation);
				// now we'd like to rotate bigDir around the smalldir axis
				// use our rotation indexer using smallDir as the primary axis
				int bigRotationIndex = BlockStateUtil.getRotationIndexForDirection(smallDir, bigDir);
				int nextBigRotationIndex = (bigRotationIndex+1) % 4;
				Direction newBigDir = BlockStateUtil.getOutputDirection(smallDir, nextBigRotationIndex);
				// now we just need to find the rotation index that preserves smalldir
				int newSmallRotation = BlockStateUtil.getRotationIndexForDirection(newBigDir, smallDir);
				newState = state.setValue(ATTACHMENT_DIRECTION, newBigDir)
					.setValue(ROTATION, newSmallRotation);
			}
			else
			{
				// rotate around big gear
				int newRotation = (state.getValue(ROTATION) + 1) % 4;
				newState = state.setValue(ROTATION, newRotation);
			}
			level.setBlockAndUpdate(pos, newState);
		}
		
		return isPlayerHoldingWrench ? InteractionResult.SUCCESS : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}
	
	private static void transferItem(BlockState state, BlockPos pos, Level level)
	{
		Direction outputDir = state.getValue(ATTACHMENT_DIRECTION);
		BlockPos outputPos = pos.relative(outputDir);
		Direction inputDir = outputDir.getOpposite();
		BlockPos inputPos = pos.relative(inputDir);

		IItemHandler inputHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, outputDir);
		if (inputHandler != null)
		{
			IItemHandler outputHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, outputPos, inputDir);
			
			// if the input handler exists and either the output handler exists or we have room to eject the item
			if (outputHandler != null || !level.getBlockState(outputPos).isCollisionShapeFullBlock(level, outputPos))
			{
				ItemStack stack = extractNextStack(inputHandler);
				if (stack.getCount() > 0)
				{
					ItemStack remaining = outputHandler == null ? stack : putStackInHandler(stack, outputHandler);
					WorldHelper.ejectItemstack(level, pos, outputDir, remaining);
				}
			}
		}
	}
	
	private static ItemStack extractNextStack(IItemHandler handler)
	{
		int slots = handler.getSlots();
		for (int i=0; i<slots; i++)
		{
			ItemStack stack = handler.extractItem(i, 64, false);
			if (stack.getCount() > 0)
			{
				return stack.copy();
			}
		}
		return ItemStack.EMPTY;
	}
	
	private static ItemStack putStackInHandler(ItemStack stack, IItemHandler handler)
	{
		ItemStack remaining = stack.copy();
		int slots = handler.getSlots();
		for (int i=0; i<slots; i++)
		{
			remaining = handler.insertItem(i, remaining, false);
			if (remaining.getCount() <= 0)
			{
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().extractorEntity.get().create(pos,state);
	}

	private static final BlockEntityTicker<GenericBlockEntity> TICKER = ExtractorBlock::serverTick;
	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return (!level.isClientSide) && type == MoreRed.get().extractorEntity.get()
			? (BlockEntityTicker<T>)TICKER
			: null;
	}
	
	public static void serverTick(Level level, BlockPos pos, BlockState state, GenericBlockEntity be)
	{		
		Map<NodeShape, MechanicalState> nodes = be.getData(MechanicalNodeStates.HOLDER.get());
		// if missing mechanical data, skip
		if (nodes == null)
		{
			return;
		}
		Direction attachDir = state.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
		Direction inputDir = attachDir.getOpposite();
		MechanicalState mechanicalState = nodes.getOrDefault(NodeShape.ofSide(inputDir), MechanicalState.ZERO);
		// we want to detect when rotation passes 0
		// when velocity is positive, radians should have been <0 last tick
		// when velocity is positive, radians should have been >0 last tick
		double radiansPerSecond = mechanicalState.angularVelocity();
		if (radiansPerSecond == 0D)
			return;
		double absRadiansPerSecond = Math.abs(radiansPerSecond);
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		double seconds = gameTimeTicks * 0.05D;
		double radians = absRadiansPerSecond * seconds;
		double simpleRadians = radians % (Math.TAU); // gives a value in the range [0, 2PI)
		// check if we passed 0 this tick
		double radiansPerTick = absRadiansPerSecond * 0.05D;
		double radiansLastTick = simpleRadians - radiansPerTick;
		if (radiansLastTick < 0D) // we made a full revolution this tick
		{
			transferItem(state,pos,level);
		}
	}
	
	public static Map<NodeShape,MechanicalState> normalizeMachine(BlockState state, HolderLookup.Provider provider, Map<NodeShape,MechanicalState> runtimeData)
	{
		// let the default state point to down (attachment direction) and north+south (the axles)
		// the input direction (which has to spin) is the opposite of the attachment direction (so it faces up)
		// so, no matter which way we're pointing in-world,
		// store those two states in those three directions
		Map<NodeShape,MechanicalState> result = new HashMap<>();
		
		// input is easier, we can get that directly from the attachment property
		Direction attachDir = state.getValue(ATTACHMENT_DIRECTION);
		Direction inputDir = attachDir.getOpposite();
		MechanicalState inputState = runtimeData.getOrDefault(NodeShape.ofSide(inputDir), MechanicalState.ZERO);
		result.put(NodeShape.ofSide(Direction.UP), inputState);
		
		// axle is trickier, but we can reuse the plate block utils for that
		// let primary axle = "output direction"
		int rotation = state.getValue(ROTATION);
		Direction axleDir = BlockStateUtil.getOutputDirection(attachDir, rotation);
		Direction reverseAxleDir = axleDir.getOpposite();
		result.put(NodeShape.ofSide(Direction.NORTH), runtimeData.getOrDefault(NodeShape.ofSide(axleDir), MechanicalState.ZERO));
		result.put(NodeShape.ofSide(Direction.SOUTH), runtimeData.getOrDefault(NodeShape.ofSide(reverseAxleDir), MechanicalState.ZERO));
		return result;
	}
	
	public static Map<NodeShape,MechanicalState> denormalizeMachine(BlockState state, HolderLookup.Provider provider, Map<NodeShape,MechanicalState> diskData)
	{
		Map<NodeShape,MechanicalState> result = new HashMap<>();
		MechanicalState inputState = diskData.getOrDefault(NodeShape.ofSide(Direction.UP), MechanicalState.ZERO);
		MechanicalState axleState = diskData.getOrDefault(NodeShape.ofSide(Direction.NORTH), MechanicalState.ZERO);
		MechanicalState reverseAxleState = diskData.getOrDefault(NodeShape.ofSide(Direction.SOUTH), MechanicalState.ZERO);
		Direction attachDir = state.getValue(ATTACHMENT_DIRECTION);
		Direction inputDir = attachDir.getOpposite();
		Direction axleDir = BlockStateUtil.getOutputDirection(attachDir, state.getValue(ROTATION));
		Direction reverseAxleDir = axleDir.getOpposite();
		result.put(NodeShape.ofSide(inputDir), inputState);
		result.put(NodeShape.ofSide(axleDir), axleState);
		result.put(NodeShape.ofSide(reverseAxleDir), reverseAxleState);
		return result;
	}

	@Override
	public boolean hasBlockStateModelsForPlacementPreview(BlockState state)
	{
		return false;
	}
	
}
