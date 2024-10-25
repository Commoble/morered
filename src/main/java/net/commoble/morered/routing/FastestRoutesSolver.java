package net.commoble.morered.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.commoble.morered.transportation.TubeBlock;
import net.commoble.morered.transportation.TubeBlockEntity;
import net.commoble.morered.util.PosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Used by the routing network to determine the routes to all endpoints from a given tube in the network
 * @author Joseph
 *
 */
public class FastestRoutesSolver
{
	/**
	 * returns a List of Routes, sorted in the order of shortest-route-from-the-start-pos first
	 * @param startPos
	 * @return
	 */
	public static List<Route> generateRoutes(RoutingNetwork network, Level world, BlockPos startPos)
	{
		// currently uses Djikstra's algorithm
		// this necessitates that all positions are looked at to guarantee the shortest paths are found
		// (if all tubes had length 1 we could just do breadth-first search,
			// but the potential existence of teleporting tubes or tubes with greater than
			// one meter per meter requires we look at all tubes)
		
		// mapping of pos : shortest distance from start to pos
		// traditionally (for djikstra's) we initialize the map with position:infinity for all positions
		// we'll just treat "not in map" = infinity
		// once we find all of these, we use the network's endpoints to put the routes together
		Object2IntOpenHashMap<BlockPos> tubeDists = new Object2IntOpenHashMap<BlockPos>();
		tubeDists.put(startPos, 0);	// put the startpos in the map
		Object2IntOpenHashMap<Endpoint> endpointDists = new Object2IntOpenHashMap<Endpoint>();
		
		HashSet<BlockPos> visitedTubes = new HashSet<BlockPos>();
		HashSet<Endpoint> visitedEndpoints = new HashSet<Endpoint>();
		
		// mapping of position U : previous position immediately before it on the shortest path from start to U
		// these all start as null or not in map
		HashMap<BlockPos, BlockPos> tubePrevs = new HashMap<BlockPos, BlockPos>();
		HashMap<Endpoint, BlockPos> endpointPrevs = new HashMap<Endpoint, BlockPos>();
		
		// queue of all positions that remain to be looked at, sorted by distance (closest to start first)
		// Routes implement Comparable so this will keep itself sorted
		PriorityQueue<PosAndDist> distQueue = new PriorityQueue<PosAndDist>(network.getSize());
		distQueue.add(new PosAndDist(startPos, 0));
		
		// loop until we've emptied the queue (i.e. looked at every position
		while (!distQueue.isEmpty())
		{
			// the next pos in the queue is the nearest one to the starting position
			PosAndDist node = distQueue.poll();	// this won't be null as the queue is not empty
			visitedTubes.add(node.pos);	// mark it as visited; only tubes will be placed in the distqueue
			
			// for each position nextPos adjacent to pos, if that position is a tube or endpoint in the network
			// remove it from the queue
			// TODO handle support for noneuclidean tubes
			BlockState state = world.getBlockState(node.pos);
			@Nullable TubeBlockEntity tube = world.getBlockEntity(node.pos) instanceof TubeBlockEntity tubeTe ? tubeTe : null;
			Set<Direction> dirs = tube == null
				? TubeBlock.getConnectedDirections(state)
				: tube.getAllConnectedDirections();

			for (Direction face : dirs)
			{
				BlockPos checkPos = tube == null
					? node.pos.relative(face)
					: tube.getConnectedPos(face);
				Endpoint maybeEndpoint = new Endpoint(checkPos, face.getOpposite());
				
				
				if (!visitedTubes.contains(checkPos) && network.tubes.contains(checkPos))
				{
					int newDist = node.dist + 1;
					// if the distance through the polled node is shorter than the shortest known distance to the given neighbor
					// (or the neighbor hasn't been visited yet)
					// update the known distance to the neighbor, and update the prev tracker
					if (!tubeDists.containsKey(checkPos) || newDist < tubeDists.getInt(checkPos))
					{
						tubeDists.put(checkPos, newDist);
						tubePrevs.put(checkPos, node.pos);
					}
					
					// either way, as long as it hasn't been visited yet, the neighbor goes into the queue
					distQueue.add(new PosAndDist(checkPos, tubeDists.getInt(checkPos)));
				}
				else if (!visitedEndpoints.contains(maybeEndpoint) && network.endpoints.contains(maybeEndpoint))
				{
					visitedEndpoints.add(maybeEndpoint);
					int newDist = node.dist+1;
					if (!endpointDists.containsKey(maybeEndpoint) || newDist < endpointDists.getInt(maybeEndpoint))
					{
						endpointDists.put(maybeEndpoint, newDist);
						endpointPrevs.put(maybeEndpoint, node.pos);
					}
					// don't add endpoint to distqueue since endpoints are singlesided
				}
			}
			
			// if nextPos.dist > pos.dist + 1
			// set nextPos.dist to pos.dist + 1
			
			// prev(nextPos) = pos
			// readd nextPos to the queue, with the updated distance
		}
		
		// now our network is solved, use the data we've collected to determine the route to the each endpoint
		// we want to return a sorted collection of Routes that can be iterated over
		// priority queues can't be iterated over properly; we can either assemble a PQ and convert it to a list
		// or just start with a list and then sort it
		// analysis of each implementation (ignoring the time it takes to create the routes themselves):
			// List:
				// time to add a route to list is constant; time to add n routes is O(n)
				// time to sort it is O(n*logn) at most
				// worst-case time complexity is O(n*logn)
			// PQ:
				// time to add a route to the PQ is O(logn); time to add n routes is O(n*logn)
				// time to remove a route from the assembled PQ is O(n); time to remove n routes is O(n^2)
				// worst-case time complexity is O(n^2)
		// this is oversimplified but just using a list will probably be better

		List<Route> routes = new ArrayList<Route>(network.endpoints.size());
		
		// for each endpoint, determine the route to it and add it to the list
		for (Endpoint endpoint : network.endpoints)
		{
			LinkedList<Direction> sequenceOfMoves = getSequenceOfMoves(world, endpoint, startPos, new LinkedList<Direction>(), tubePrevs, endpointPrevs);
			if (sequenceOfMoves != null)
			{
				routes.add(new Route(endpoint, sequenceOfMoves.size(), sequenceOfMoves));
			}
		}
		
		routes.sort(null);
		
		return routes;
	}
	
	private static LinkedList<Direction> getSequenceOfMoves(Level world, Endpoint endpoint, BlockPos startPos, LinkedList<Direction> returnList, HashMap<BlockPos, BlockPos> tubePrevs, HashMap<Endpoint, BlockPos> endpointPrevs)
	{
		if (!endpointPrevs.containsKey(endpoint))
		{
			return null;
		}
		BlockPos prevPos = endpointPrevs.get(endpoint);
		returnList.addFirst(endpoint.face.getOpposite());
		
		if (prevPos.equals(startPos))
		{
			return returnList;
		}
		else
		{
			return getSequenceOfMoves(world, prevPos, startPos, returnList, tubePrevs);
		}
	}
	
	// recursively assemble the sequence of moves required to get to a given position from the startPos
	private static LinkedList<Direction> getSequenceOfMoves(Level world, BlockPos pos, BlockPos startPos, LinkedList<Direction> returnList, HashMap<BlockPos, BlockPos> prevs)
	{
		
		if (!prevs.containsKey(pos))
		{
			return null;	// if the endpoint can't be reached from the start point, then we don't create a route to it 
		}
		
		BlockPos prevPos = prevs.get(pos);
		returnList.addFirst(PosHelper.getTravelDirectionFromTo(world, prevPos, pos));
		
		// TODO at the moment we're going to blindly trust that the route solver didn't create any loops, may need to add safeguard later
		// to be honest if we do run into a loop, that's a problem to be fixed in the route solver, not here
		if (prevPos.equals(startPos))
		{
			return returnList;
		}
		else
		{
			return getSequenceOfMoves(world, prevPos, startPos, returnList, prevs);
		}
	}
}
