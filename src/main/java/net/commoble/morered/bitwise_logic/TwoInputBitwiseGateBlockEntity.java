package net.commoble.morered.bitwise_logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.InputSide;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TwoInputBitwiseGateBlockEntity extends BitwiseGateBlockEntity
{
	public static final String CLOCKWISE_INPUT = "clockwise_input";
	public static final String COUNTERCLOCKWISE_INPUT = "counterclockwise_input";
	
	protected int clockwiseInput = 0;
	protected int counterClockwiseInput = 0;

	public TwoInputBitwiseGateBlockEntity(BlockEntityType<? extends TwoInputBitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public static TwoInputBitwiseGateBlockEntity create(BlockPos pos, BlockState state)
	{
		return new TwoInputBitwiseGateBlockEntity(MoreRed.TWO_INPUT_BITWISE_GATE_BLOCK_ENTITY.get(), pos, state);
	}
	
	public void setClockwiseInputOnChannel(boolean hasInput, int channel)
	{
		int newInput = this.clockwiseInput;
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setClockwiseInput(newInput, false);
	}
	
	public void setClockwiseInput(int input, boolean forceOutputUpdate)
	{
		int oldInput = this.clockwiseInput;
		this.clockwiseInput = input;
		if (forceOutputUpdate || oldInput != this.clockwiseInput)
		{
			this.updateOutput();
			this.setChanged();
		}
	}
	
	public void setCounterclockwiseInputOnChannel(boolean hasInput, int channel)
	{
		int newInput = this.counterClockwiseInput;
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setCounterclockwiseInput(newInput, false);
	}
	
	public void setCounterclockwiseInput(int input, boolean forceOutputUpdate)
	{
		int oldInput = this.counterClockwiseInput;
		this.counterClockwiseInput = input;
		if (forceOutputUpdate || oldInput != this.counterClockwiseInput)
		{
			this.updateOutput();
			this.setChanged();
		}
	}

	@Override
	protected void resetOutput()
	{
		this.updateOutput();
	}
	
	public void updateOutput()
	{
		if (this.getBlockState().getBlock() instanceof TwoInputBitwiseGateBlock block)
		{
			int output = block.operator.apply(this.clockwiseInput, 0, counterClockwiseInput);
			this.setOutput(output);
		}
	}

	@Override
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		output.putInt(CLOCKWISE_INPUT, this.clockwiseInput);
		output.putInt(COUNTERCLOCKWISE_INPUT, this.counterClockwiseInput);
	}

	@Override
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		this.clockwiseInput = input.getIntOr(CLOCKWISE_INPUT,0);
		this.counterClockwiseInput = input.getIntOr(COUNTERCLOCKWISE_INPUT,0);
	}

	@Override
	protected List<TransmissionNode> createInputNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel, int channelIndex, Direction attachmentDir, Direction outputDir)
	{
		int rotationIndex = state.getValue(PlateBlockStateProperties.ROTATION);
		Direction clockwiseSide = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
		Direction counterClockwiseSide = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
		NodeShape clockwiseShape = NodeShape.ofSideSide(attachmentDir, clockwiseSide);
		NodeShape counterClockwiseShape = NodeShape.ofSideSide(attachmentDir, counterClockwiseSide);
		NodeShape clockwiseNeighborShape = NodeShape.ofSideSide(attachmentDir, counterClockwiseSide);
		NodeShape counterClockwiseNeighborShape = NodeShape.ofSideSide(attachmentDir, clockwiseSide);
		SignalGraphKey clockwiseNeighborNode = new SignalGraphKey(levelKey, pos.relative(clockwiseSide), clockwiseNeighborShape, channel);
		SignalGraphKey counterClockwiseNeighborNode = new SignalGraphKey(levelKey, pos.relative(counterClockwiseSide), counterClockwiseNeighborShape, channel);
		
		return List.of(
			new TransmissionNode(
				clockwiseShape,
				BitwiseGateBlockEntity.INPUT_SOURCE,
				Set.of(clockwiseSide),
				Set.of(clockwiseNeighborNode),
				(levelAccess, power) -> {
					this.setClockwiseInputOnChannel(power > 0, channelIndex);
					return Map.of();
				}
			),
			new TransmissionNode(
				counterClockwiseShape,
				BitwiseGateBlockEntity.INPUT_SOURCE,
				Set.of(counterClockwiseSide),
				Set.of(counterClockwiseNeighborNode),
				(levelAccess, power) -> {
					this.setCounterclockwiseInputOnChannel(power > 0, channelIndex);
					return Map.of();
				}
			)
		);
	}
	
}
