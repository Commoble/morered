package net.commoble.morered.future;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.state.BlockState;

public record WireGraph(Map<NodePos, TransmissionNode> nodesInGraph, Set<BlockPos> blocksInGraph, List<BiConsumer<LevelAccessor,Integer>> receiverNodes, int power)
{
	public static WireGraph fromOriginNode(ServerLevel level, NodePos originNodePos, TransmissionNode originNode, Map<BlockPos, StateWirer> knownWirers)
	{
		// build graph
		Map<NodePos, TransmissionNode> nodesInGraph = new HashMap<>();
		Set<BlockPos> blocksInGraph = new HashSet<>();
		Queue<NodeAtPos> uncheckedNodesInGraph = new LinkedList<>();
		List<BiConsumer<LevelAccessor,Integer>> receiverNodes = new ArrayList<>();
		nodesInGraph.put(originNodePos, originNode);
		blocksInGraph.add(originNodePos.face().pos());
		uncheckedNodesInGraph.add(new NodeAtPos(originNodePos, originNode));
		int maxSize = ExperimentalModEvents.COMMON_CONFIG.maxWireNetworkSize().getAsInt();
		Function<BlockPos, StateWirer> wirerLookup = targetPos -> knownWirers.computeIfAbsent(targetPos, pos -> {
			BlockState state = level.getBlockState(targetPos);
			@SuppressWarnings("deprecation")
			Wirer actualWirer = BuiltInRegistries.BLOCK.getData(ExperimentalModEvents.WIRER_DATA_MAP, state.getBlock().builtInRegistryHolder().getKey());
			return new StateWirer(state, actualWirer == null ? DefaultWirer.INSTANCE : actualWirer);
		});
		SignalGetter signalGetter = new WireIgnoringSignalGetter(level, wirerLookup);
		
		int highestPowerFound = 0;
		
		// iterate over the list of nodes we have added to the graph
		// "next" node: in the graph but not processed neighbors yet
		whileLoop:
		while (uncheckedNodesInGraph.poll() instanceof NodeAtPos(NodePos(Face nextFace, Channel nextChannel), TransmissionNode nextNode))
		{
			// process next node's neighbors
			// "target" node: a node which next node can conceivably connect to, and may form a mutual connection, in which case it should be in the graph
			for (Channel targetChannel : nextChannel.expand())
			{
				// look for target nodes
				for (Face targetFace : nextNode.connectableNodes())
				{
					NodePos targetNodePos = new NodePos(targetFace, targetChannel);
					// skip target if it's already in the graph
					if (!nodesInGraph.containsKey(targetNodePos))
					{
						BlockPos targetPos = targetFace.pos();
						StateWirer targetStateWirer = wirerLookup.apply(targetPos);
						BlockState targetState = targetStateWirer.state();
						Wirer targetWirer = targetStateWirer.wirer();
						
						Direction targetSide = targetFace.attachmentSide();
						@Nullable TransmissionNode targetNode = targetWirer.getTransmissionNodes(level, targetPos, targetState, targetSide).get(targetChannel);
						if (targetNode != null)
						{
							// target node exists! but can it connect back?
							if (targetNode.connectableNodes().contains(nextFace))
							{
								// yes
								nodesInGraph.put(targetNodePos, targetNode);
								blocksInGraph.add(targetPos);
								uncheckedNodesInGraph.add(new NodeAtPos(targetNodePos, targetNode));
								if (nodesInGraph.size() >= maxSize)
									break whileLoop;
							}
						}
						
						// check receiver nodes, we need to remember the listeners
						@Nullable BiConsumer<LevelAccessor,Integer> targetReceiver = targetWirer.getReceiverEndpoints(level, targetPos, targetState, targetSide, nextFace).get(targetChannel);
						if (targetReceiver != null)
						{
							receiverNodes.add(targetReceiver);
						}
						
						// check supplier nodes too
						if (highestPowerFound < 15)
						{
							powerLoop:
							for (var channelPower : targetWirer.getSupplierEndpoints(level, targetPos, targetState, targetSide, nextFace).entrySet())
							{
								if (channelPower.getKey() == targetChannel)
								{
									int suppliedPower = channelPower.getValue().apply(level);
									if (suppliedPower > highestPowerFound)
									{
										highestPowerFound = suppliedPower;
										if (highestPowerFound >= 15)
											break powerLoop;
									}
								}
							}
						}
					}
				}
			}
		}
		
		// calculate highest input power
		// these are the places input power can come from:
		// A) supplier endpoint connected to a transmission node
		// B) vanilla power reader supplied by a transmission node
		// highest power level is 15, if we observe this then we can skip the rest of the graph
		// but if our graph is too large then we ignore power and set everything to 0
		// we delay this until after the graph is built because we do not read vanilla signals from blockspos in the graph
		int power;
		if (nodesInGraph.size() >= maxSize)
		{
			power = 0;
		}
		else if (highestPowerFound >= 15)
		{
			power = 15;
		}
		else
		{
			powerReaderLoop:
			for (var entry : nodesInGraph.entrySet())
			{
				NodePos nodePos = entry.getKey();
				TransmissionNode node = entry.getValue();
				Face nodeFace = nodePos.face();
				BlockPos nodeBlockPos = nodeFace.pos();
				for (Direction directionToNeighbor : node.powerReaders())
				{
					BlockPos neighborPos = nodeBlockPos.relative(directionToNeighbor);
					if (!blocksInGraph.contains(neighborPos))
					{
						int neighborPower = signalGetter.getSignal(nodeBlockPos.relative(directionToNeighbor), directionToNeighbor);
						if (neighborPower > highestPowerFound)
						{
							highestPowerFound = neighborPower;
							if (highestPowerFound >= 15)
								break powerReaderLoop;
						}
					}
				}
			}
			
			power = highestPowerFound;
		}
		
		return new WireGraph(nodesInGraph, blocksInGraph, receiverNodes, power);
	}
	
	public boolean hasTransmissionNode(NodePos nodePos)
	{
		return nodesInGraph().containsKey(nodePos);
	}
	
	public static boolean isBlockInAnyGraph(BlockPos pos, Collection<WireGraph> graphs)
	{
		for (WireGraph graph : graphs)
		{
			if (graph.blocksInGraph().contains(pos))
				return true;
		}
		return false;
	}
	
	public Map<Face, SignalStrength> updateListeners(ServerLevel serverLevel)
	{
		Map<Face, SignalStrength> neighborUpdatingNodes = new HashMap<>();
		
		for (var entry : this.nodesInGraph.entrySet())
		{
			NodePos nodePos = entry.getKey();
			BlockPos nodeBlockPos = nodePos.face().pos();
			TransmissionNode node = entry.getValue();
			node.graphListener().apply(serverLevel, this.power).forEach((directionToNeighbor, signalStrength) -> {
				neighborUpdatingNodes.merge(new Face(nodeBlockPos, directionToNeighbor), signalStrength, SignalStrength::max);
			});
		}
		
		for (BiConsumer<LevelAccessor, Integer> receiverNode : this.receiverNodes)
		{
			receiverNode.accept(serverLevel, this.power);
		}
		
		return neighborUpdatingNodes;
	}
}
