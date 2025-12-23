package net.commoble.morered.bitwise_logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaGameEvents;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.SignalStrength;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BitwiseGateBlockEntity extends BlockEntity
{
	public static final String OUTPUT = "power";
	private static final ToIntFunction<LevelReader> ON = level -> 15;
	private static final ToIntFunction<LevelReader> OFF = level -> 0;
	private static final BiFunction<LevelAccessor,Integer,Map<Direction, SignalStrength>> OUTPUT_LISTENER = (level,power) -> Map.of();
	protected static final ToIntFunction<LevelReader> INPUT_SOURCE = (level) -> 0;
	public static record ChannelInfo(int index, Function<BitwiseGateBlockEntity,ToIntFunction<LevelReader>> sourceFunction) {}
	private static final Map<Channel,ChannelInfo> CHANNEL_INFOS = Util.make(new HashMap<>(), map -> {
		for (DyeColor color : DyeColor.values())
		{
			Channel channel = Channel.single(color);
			int index = color.ordinal();
			
			map.put(channel, new ChannelInfo(index, gate -> level ->
				(gate.getOutput() & (1 << index)) != 0
					? 15
					: 0));
		}
	});
	
	protected int output = 0;
	protected Map<Channel,Collection<TransmissionNode>> nodesByChannel = new HashMap<>();
	
	protected abstract List<TransmissionNode> createInputNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel, int channelIndex, Direction attachmentDir, Direction outputDir);
	protected abstract void resetOutput();
	
	public BitwiseGateBlockEntity(BlockEntityType<? extends BitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public int getOutput()
	{
		return this.output;
	}
	
	public boolean hasOutput(int channel)
	{
		return (this.output & (1 << channel)) != 0;
	}
	
	public void setOutput(int output)
	{
		int oldOutput = this.output;
		this.output = output;
		if (oldOutput != this.output)
		{
			this.setChanged();
			ExMachinaGameEvents.scheduleSignalGraphUpdate(level, worldPosition);
		}
	}
	
	protected Map<Channel, ToIntFunction<LevelReader>> createSupplierEndpoints()
	{
		final int output = this.getOutput();
		Map<Channel, ToIntFunction<LevelReader>> map = new HashMap<>();
		for (DyeColor color : DyeColor.values())
		{
			final var power = (output & (1 << color.ordinal())) != 0
				? ON
				: OFF;
			map.put(Channel.single(color), power);
		}
		
		return map;
	}

	@Override
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		output.putInt(OUTPUT, this.output);
	}
	
	@Override
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		this.output = input.getIntOr(OUTPUT,0);
	}

	public Collection<TransmissionNode> getTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel)
	{
		Collection<TransmissionNode> result = this.nodesByChannel.get(channel);
		if (result == null)
		{
			result = this.createTransmissionNodes(levelKey, level, pos, state, channel);
			this.nodesByChannel.put(channel,result);
		}
		return result;
	}

	protected Collection<TransmissionNode> createTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel)
	{
		List<TransmissionNode> transmissionNodes = new ArrayList<>();
		var channelInfo = CHANNEL_INFOS.get(channel);
		if (channelInfo == null)
			return transmissionNodes;
		
		Direction attachmentDir = state.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(state);
		NodeShape outputShape = NodeShape.ofSideSide(attachmentDir, primaryOutputDirection);
		BlockPos outputNeighborPos = pos.relative(primaryOutputDirection);
		
		// start with the output node, we have exactly one of those		
		transmissionNodes.add(new TransmissionNode(
			outputShape,
			channelInfo.sourceFunction().apply(this),
			Set.of(),
			Set.of(new SignalGraphKey(levelKey, outputNeighborPos, NodeShape.ofSideSide(attachmentDir, primaryOutputDirection.getOpposite()), channel)),
			OUTPUT_LISTENER
		));
		
		List<TransmissionNode> inputNodes = this.createInputNodes(levelKey, level, pos, state, channel, channelInfo.index(), attachmentDir, primaryOutputDirection);
		transmissionNodes.addAll(inputNodes);
		
		return transmissionNodes;
	}

	@Override
	@Deprecated
	public void setBlockState(BlockState state)
	{
		super.setBlockState(state);
		this.resetOutput();
		this.resetNodes();
		ExMachinaGameEvents.scheduleSignalGraphUpdate(level, worldPosition);
	}
	
	@Override
	public void setLevel(Level level)
	{
		super.setLevel(level);
		this.resetOutput();
		this.resetNodes();
		ExMachinaGameEvents.scheduleSignalGraphUpdate(level, worldPosition);
	}
	
	public void resetNodes()
	{
		this.nodesByChannel = new HashMap<>();
	}
}
