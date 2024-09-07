package net.commoble.morered.future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;

@EventBusSubscriber(modid=MoreRed.MODID)
public class ExperimentalGameEvents
{
	@SubscribeEvent
	public static void onVanillaGameEvent(VanillaGameEvent event)
	{
		// called when a block update occurs at a given position (including when a blockstate change occurs at that position)
		// if the blockstate changed, the event's given state is the new blockstate
		LevelAccessor level = event.getLevel();
		
		if (level instanceof ServerLevel serverLevel && event.getVanillaEvent().is(ExperimentalModEvents.WIRE_UPDATE))
		{
			onWireUpdate(serverLevel, BlockPos.containing(event.getEventPosition()));
		}
	}
	
	@SuppressWarnings("deprecation")
	private static void onWireUpdate(ServerLevel level, BlockPos originPos)
	{
		// collect all of the nodes in this block
		BlockState originState = level.getBlockState(originPos);
		Block originBlock = originState.getBlock();
		Wirer originWirer = BuiltInRegistries.BLOCK.getData(ExperimentalModEvents.WIRER_DATA_MAP, originBlock.builtInRegistryHolder().key());
		Map<NodePos, TransmissionNode> originNodes = new HashMap<>();
		for (Direction face : Direction.values()) {
			originWirer.getTransmissionNodes(level, originPos, originState, face).forEach((channel,node) -> {
				originNodes.put(new NodePos(new Face(originPos, face), channel), node);
			});
		}
		
		// construct graph from each origin node
		List<WireGraph> graphs = new ArrayList<>();
		Map<BlockPos, StateWirer> knownWirers = new HashMap<>();
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
		Set<Face> nodesUpdatingNeighbors = new HashSet<>();
		for (WireGraph graph : graphs)
		{
			nodesUpdatingNeighbors.addAll(graph.updateListeners(level));
		}
		
		// invoke block updates on blocks which are adjacent to the graph but have no transmission nodes within it
		for (Face updatedNodeFace : nodesUpdatingNeighbors)
		{
			BlockPos nodeBlockPos = updatedNodeFace.pos();
			Direction directionToNeighbor = updatedNodeFace.attachmentSide();
			BlockPos neighborPos = nodeBlockPos.relative(directionToNeighbor);
			if (!WireGraph.isBlockInAnyGraph(neighborPos, graphs))
			{
				level.neighborChanged(neighborPos, level.getBlockState(nodeBlockPos).getBlock(), nodeBlockPos);
			}
		}
	}
}
