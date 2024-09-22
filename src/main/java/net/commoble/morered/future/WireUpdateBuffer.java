package net.commoble.morered.future;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public class WireUpdateBuffer extends SavedData
{
	public static final String ID = "morered/wireupdatebuffer";
	
	public static final SavedData.Factory<WireUpdateBuffer> FACTORY = new SavedData.Factory<>(WireUpdateBuffer::new, (tag,registries) -> new WireUpdateBuffer());
	
	private Set<BlockPos> positions = new HashSet<>();
	
	public static WireUpdateBuffer get(ServerLevel world)
	{
		return world.getDataStorage().computeIfAbsent(FACTORY, ID);
	}
	
	public void enqueue(BlockPos pos)
	{
		this.positions.add(pos);
	}
	
	public void tick(ServerLevel level)
	{
		if (this.positions.isEmpty())
			return;

		Set<BlockPos> originPositions = this.positions;
		this.positions = new HashSet<>();
		
		// construct graph from each origin node
		List<WireGraph> graphs = new ArrayList<>();
		Map<BlockPos, StateWirer> knownWirers = new HashMap<>();
		Map<NodePos, TransmissionNode> originNodes = new HashMap<>();
		
		for (BlockPos originPos : originPositions)
		{
			// collect all of the nodes in this block
			BlockState originState = level.getBlockState(originPos);
			Block originBlock = originState.getBlock();
			Wirer originWirer = BuiltInRegistries.BLOCK.getData(ExperimentalModEvents.WIRER_DATA_MAP, originBlock.builtInRegistryHolder().key());
			if (originWirer == null)
				continue;
			
			for (Direction face : Direction.values()) {
				originWirer.getTransmissionNodes(level, originPos, originState, face).forEach((channel,node) -> {
					originNodes.put(new NodePos(new Face(originPos, face), channel), node);
				});
			}
		}
		originNodes.forEach((nodePos, node) -> {
			// as we construct graphs, ignore origin nodes that exist in existing graphs
			for (WireGraph priorGraph : graphs)
			{
				if (priorGraph.hasTransmissionNode(nodePos))
				{
					return;
				}
			}
			
			WireGraph graph = WireGraph.fromOriginNode(level, nodePos, node, knownWirers);
			graphs.add(graph);
		});
		
		// each graph has calculated the highest input power into the graph
		
		// inform all listeners in all graphs of the new power
		// keep track of neighbors to update
		// how do neighbor updates work?
		// each time we proc a listener on a transition node at nodepos, it gives us a set of directions to proc neighbor updates in
		// we store these as a face (nodePos+direction)
		Map<Face, SignalStrength> nodesUpdatingNeighbors = new HashMap<>();
		for (WireGraph graph : graphs)
		{
			graph.updateListeners(level).forEach((face,signalStrength) -> nodesUpdatingNeighbors.merge(face, signalStrength, SignalStrength::max));
		}
		
		// invoke block updates on blocks which are adjacent to the graph but have no transmission nodes within it
		nodesUpdatingNeighbors.forEach((updatedNodeFace, signalStrength) -> {
			BlockPos nodeBlockPos = updatedNodeFace.pos();
			Direction directionToNeighbor = updatedNodeFace.attachmentSide();
			BlockPos neighborPos = nodeBlockPos.relative(directionToNeighbor);
			if (!WireGraph.isBlockInAnyGraph(neighborPos, graphs))
			{
				Block nodeBlock = level.getBlockState(nodeBlockPos).getBlock();
				level.neighborChanged(neighborPos, nodeBlock, nodeBlockPos);
				if (signalStrength == SignalStrength.STRONG)
				{
					level.updateNeighborsAtExceptFromFacing(neighborPos, nodeBlock, directionToNeighbor.getOpposite());
				}
			}
		});
	}

	@Override
	public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries)
	{
		return compound; //noop
	}

	@Override
	public void save(File file, Provider registries)
	{
		// no
	}
	
	
	
}
