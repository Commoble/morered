package net.commoble.morered.plate_blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.commoble.morered.Names;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public enum RedstonePlateSignalComponent implements SignalComponent
{
	INSTANCE;
	
	public static final ResourceKey<MapCodec<? extends SignalComponent>> RESOURCE_KEY = ResourceKey.create(ExMachinaRegistries.SIGNAL_COMPONENT_TYPE, MoreRed.id(Names.REDSTONE_PLATE));
	public static final MapCodec<RedstonePlateSignalComponent> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends SignalComponent> codec()
	{
		return CODEC;
	}

	@Override
	public Collection<TransmissionNode> getTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel)
	{
		List<TransmissionNode> transmissionNodes = new ArrayList<>();
		// annoying hack to keep bundled cables from shorting themselves on logic plates
		if (channel == Channel.redstone())
			return transmissionNodes;
		
		// TODO refactor getOutputSides to accept BlockGetter in 26.2+
		if (state.getBlock() instanceof RedstonePlateBlock block)
		{
			Direction attachmentDir = state.getValue(PlateBlock.ATTACHMENT_DIRECTION);
			for (InputSide inputSide : block.getInputSides())
			{
				int baseRotation = state.getValue(PlateBlock.ROTATION);
				Direction inputDir = BlockStateUtil.getInputDirection(attachmentDir, baseRotation, inputSide.rotationsFromOutput);
				Direction directionFromNeighbor = inputDir.getOpposite();
				BlockPos neighborPos = pos.relative(inputDir);
				transmissionNodes.add(new TransmissionNode(
					NodeShape.ofSideSide(attachmentDir, inputDir),
					reader -> 0,
					Set.of(),
					Set.of(new SignalGraphKey(levelKey, neighborPos, NodeShape.ofSideSide(attachmentDir, directionFromNeighbor), channel)),
					(levelAccess, power) -> Map.of(),
					true
				));
			}
			for (Direction outputDir : block.getOutputSides(level, pos, state))
			{
				Direction directionFromNeighbor = outputDir.getOpposite();
				BlockPos neighborPos = pos.relative(outputDir);
				transmissionNodes.add(new TransmissionNode(
					NodeShape.ofSideSide(attachmentDir, outputDir),
					reader -> reader.getSignal(pos, directionFromNeighbor),
					Set.of(),
					Set.of(new SignalGraphKey(levelKey, neighborPos, NodeShape.ofSideSide(attachmentDir, directionFromNeighbor), channel)),
					(levelAccess, power) -> Map.of()
				));
			}
		}
		
		return transmissionNodes;
	}


	@Override
	public boolean updateSelfFromNeighborsAfterGraphUpdate(LevelReader level, BlockState state, BlockPos pos)
	{
		return true;
	}	
}
