package net.commoble.morered.bitwise_logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SingleInputBitwiseGateBlockEntity extends BitwiseGateBlockEntity
{
	public static final String INPUT = "input";
	
	protected int input = 0;

	public SingleInputBitwiseGateBlockEntity(BlockEntityType<? extends SingleInputBitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public static SingleInputBitwiseGateBlockEntity create(BlockPos pos, BlockState state)
	{
		return new SingleInputBitwiseGateBlockEntity(MoreRed.get().singleInputBitwiseGateBeType.get(), pos, state);
	}
	
	public void setInputOnChannel(boolean hasInput, int channel)
	{
		int newInput = this.input;
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setInput(newInput, false);
	}

	public void setInput(int input, boolean forceOutputUpdate)
	{
		int oldInput = this.input;
		this.input = input;
		if ((forceOutputUpdate || oldInput != this.input) && this.getBlockState().getBlock() instanceof SingleInputBitwiseGateBlock block)
		{
			int output = block.operator.apply(0, input, 0);
			this.setOutput(output);
			this.setChanged();
		}
	}

	@Override
	protected void resetOutput()
	{
		this.setInput(this.input, true);
	}

	@Override
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		output.putInt(INPUT, this.input);
	}

	@Override
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		this.input = input.getIntOr(INPUT,0);
	}

	@Override
	protected List<TransmissionNode> createInputNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel, int channelIndex, Direction attachmentDir, Direction outputDir)
	{
		Direction inputSide = outputDir.getOpposite();
		NodeShape receiverShape = NodeShape.ofSideSide(attachmentDir, inputSide);
		NodeShape receiverNeighborShape = NodeShape.ofSideSide(attachmentDir, outputDir);
		BlockPos neighborPos = pos.relative(inputSide);
		SignalGraphKey preferredNeighbor = new SignalGraphKey(levelKey, neighborPos, receiverNeighborShape, channel);
		return List.of(new TransmissionNode(
			receiverShape,
			BitwiseGateBlockEntity.INPUT_SOURCE,
			Set.of(inputSide),
			Set.of(preferredNeighbor),
			(levelAccess, power) -> {
				this.setInputOnChannel(power > 0, channelIndex);
				return Map.of();
			}
		));
	}	
}
