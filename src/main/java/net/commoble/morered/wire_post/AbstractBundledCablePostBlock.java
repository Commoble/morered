package net.commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Face;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractBundledCablePostBlock extends AbstractPostBlock implements EntityBlock
{

	protected static final VoxelShape[] CABLE_POST_SHAPES_DUNSWE = {
		Block.box(4D, 0D, 4D, 12D, 12D, 12D),
		Block.box(4D, 4D, 4D, 12D, 16D, 12D),
		Block.box(4D, 4D, 0D, 12D, 12D, 12D),
		Block.box(4D, 4D, 4D, 12D, 12D, 16D),
		Block.box(0D, 4D, 4D, 12D, 12D, 12D),
		Block.box(4D, 4D, 4D, 16D, 12D, 12D)
	};

	private final Function<BlockState, EnumSet<Direction>> connectionGetter;
	
	public AbstractBundledCablePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties);
		this.connectionGetter = connectionGetter;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().cablePostBeType.get().create(pos, state);
	}

	protected Map<Direction, Map<Channel, TransmissionNode>> createTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, WirePostBlockEntity post)
	{
		Map<Direction, Map<Channel, TransmissionNode>> allMaps = new HashMap<>();
		for (Direction face : Direction.values())
		{
			Map<Channel, TransmissionNode> map = new HashMap<>();
			for (Channel channel : Channel.SIXTEEN_COLORS)
			{
				Set<Direction> parallelDirections = this.getParallelDirections(state);
				Set<Face> connectableNodes = new HashSet<>();
				// add nodes for parallel nodes
				for (Direction directionToNeighbor : parallelDirections)
				{
					BlockPos neighborPos = pos.relative(directionToNeighbor);
					connectableNodes.add(new Face(neighborPos, face));
				}
				for (BlockPos remotePos : post.getRemoteConnections())
				{
					BlockState remoteState = level.getBlockState(remotePos);
					if (remoteState.hasProperty(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
					{
						connectableNodes.add(new Face(remotePos, remoteState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT)));
					}
				}
				TransmissionNode node = new TransmissionNode(Set.of(), connectableNodes, (levelAccess,power) -> Map.of());
				map.put(channel, node);
			}
			allMaps.put(face, map);
		}
		return allMaps;
	}
	
	public EnumSet<Direction> getParallelDirections(BlockState state)
	{
		return this.connectionGetter.apply(state);
	}
}
