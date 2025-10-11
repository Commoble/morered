package net.commoble.morered.routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.commoble.morered.transportation.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class RoutingNetwork
{
	public final Set<BlockPos> tubes = new HashSet<BlockPos>();	// set of the tubes that make up the network interior
	
	// set of the faces at the edge of the network
	// that items can be inserted into
	// the faces are the faces of the TE blocks (i.e. if face = west,
	// the network is to the west of the block, and items are inserted
	// into the west face of the block)
	public final Set<Endpoint> endpoints = new HashSet<Endpoint>();
	
	// mapping of starting blockpos : routes to all endpoints from that starting pos
	// the routes are sorted in order of nearest endpoint to the starting pos
	private final HashMap<BlockPos, List<Route>> bestRoutes = new HashMap<BlockPos, List<Route>>();
	
	public boolean invalid = false;
	
	public static final RoutingNetwork INVALID_NETWORK = new RoutingNetwork();
	static {INVALID_NETWORK.invalid = true;}
	
	private int ticksPerTube = 10;	// true value is set when the network is built

	
	// use buildNetworkFrom instead
	private RoutingNetwork()
	{

	}
	
	public int getTicksPerTube()
	{
		return this.ticksPerTube;
	}
	
	private void setTicksPerTube()
	{
		int baseDuration = MoreRed.SERVERCONFIG.ticksInTube().get();
		int size = this.tubes.size();
		int softCap = MoreRed.SERVERCONFIG.softTubeCap().get();
		int hardCap = MoreRed.SERVERCONFIG.hardTubeCap().get()+1;	// add 1 to avoid divide-by-zero errors
			// ^ this variable will always be greater than the actual network size now
		// time dilation is 1:1 if network size <= softcap
		// if size > softcap, time to traverse network becomes very large as network size approaches hardcap
		if (size < softCap)
		{
			this.ticksPerTube = baseDuration;
			return;
		}
		
		float slope = 1F/(softCap-hardCap);
		float offset = (-hardCap) * slope;
		float dilation = size * slope + offset;
		float time = 1F / (dilation * dilation);
		this.ticksPerTube = (int)(time * baseDuration);
	}
	
	/** For tubes, only pos of tube is relevant
	 * for endpoints, face is the side of the endpoint block that faces the network
	 * @param pos
	 * @param face
	 * @return
	 */
	public boolean contains(BlockPos pos, Direction face)
	{
		if (this.tubes.contains(pos))
		{
			return true;
		}
		else
		{
			for(Endpoint endpoint : this.endpoints)
			{
				if (endpoint.pos.equals(pos) && endpoint.face.equals(face))
				{
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Returns true if a blockpos can potentially be part of this network
	 * Must have a TileEntity associated with that position that is either a tube or has the inventory capability on the given side
	 * @param pos to check
	 * @param world to use
	 * @param face of the block being checked that items would be inserted into
	 * @return
	 */
	public boolean isValidToBeInNetwork(BlockPos pos, Level world, Direction face)
	{
		if (this.invalid)
			return false;
		
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof TubeBlockEntity)
			return true;
		
		return world.getCapability(Capabilities.Item.BLOCK, pos, face) != null;
	}
	
	public int getSize()
	{
		return this.tubes.size() + this.endpoints.size();
	}
	
	// gets the route to the nearest endpoint based on the position of the initial tube
	// and the side of that tube that the item was inserted into
	// returns NULL if there are no valid routes
	@Nullable
	public Route getBestRoute(Level world, BlockPos startPos, Direction insertionSide, ItemStack stack, TransactionContext context)
	{
		if (stack.getCount() <= 0)
			return null;	// can't fit round pegs in square holes
		
		// lazily generate the routes if they don't exist yet
		List<Route> routes;
		if (this.bestRoutes.containsKey(startPos))
		{
			routes = this.bestRoutes.get(startPos);
		}
		else
		{
			routes = this.generateRoutes(world, startPos);
			this.bestRoutes.put(startPos, routes);
		}
		
		// this list is sorted by travel time to an endpoint from this position
		for(Route route : routes)
		{
			// ignore the block the item was inserted from, all else are valid
			if (route.isRouteDestinationValid(world, startPos, insertionSide, stack, context))
			{
				return route;
			}
		}
		
		return null;	// no valid routes
	}
	
	private List<Route> generateRoutes(Level world, BlockPos startPos)
	{
		return FastestRoutesSolver.generateRoutes(this, world, startPos);
	}
	
	public static RoutingNetwork buildNetworkFrom(BlockPos pos, Level world)
	{
		HashSet<BlockPos> visited = new HashSet<BlockPos>();
		HashSet<BlockPos> potentialEndpoints = new HashSet<BlockPos>();
		RoutingNetwork network = new RoutingNetwork();
		//recursivelyBuildNetworkFrom(pos, world, network, visited, potentialEndpoints);
		iterativelyBuildNetworkFrom(pos, world, network, visited, potentialEndpoints);
		// we now have a set of tubes and a set of potential endpoints
		// narrow down the endpoint TEs to useable ones
		for (BlockPos endPos : potentialEndpoints)
		{			
			for(Direction face : Direction.values())
			{
				// if the te has an item handler on this face, add an endpoint (representing that face) to the network
				if (network.tubes.contains(endPos.relative(face)))
				{
					ResourceHandler<ItemResource> handler = world.getCapability(Capabilities.Item.BLOCK, endPos, face);
					if (handler != null)
					{
						network.endpoints.add(new Endpoint(endPos, face));
					}
				}
			}
		}
		
		network.confirmAllTubes(world);// make sure all tubes in this network are using this network
		network.setTicksPerTube();
		return network;
	}
	
	// very large networks throw stackoverflow if recursion is used
	private static void iterativelyBuildNetworkFrom(BlockPos startPos, Level world, RoutingNetwork network, HashSet<BlockPos> visited, HashSet<BlockPos> potentialEndpoints)
	{
		LinkedList<BlockPos> blocksToVisit = new LinkedList<BlockPos>();
		blocksToVisit.add(startPos);
		while (!blocksToVisit.isEmpty())
		{
			if (network.tubes.size() > MoreRed.SERVERCONFIG.hardTubeCap().get())
			{
				break;
			}
			
			BlockPos visitedPos = blocksToVisit.poll();
			visited.add(visitedPos);
			BlockEntity te = world.getBlockEntity(visitedPos);
			if (te instanceof TubeBlockEntity)
			{
				TubeBlockEntity tube = (TubeBlockEntity)te;
				network.tubes.add(visitedPos);
				Set<Direction> dirs = tube.getAllConnectedDirections();
				for (Direction face : dirs)
				{
					BlockPos checkPos = tube.getConnectedPos(face);
					if (!visited.contains(checkPos))
					{
						blocksToVisit.add(checkPos);
					}
				}
			}
			else if (te != null)	// te exists but isn't tube
			{
				// keep track of it for now and reconsider it later
				potentialEndpoints.add(visitedPos);
				// but don't look at it again for now
				// endpoints don't need to be visited more than once, we evaluate the sides later
			}
		}
	}
	
	// sets the network of every tube in this network to this network 
	public void confirmAllTubes(Level level)
	{
		for (BlockPos pos : this.tubes)
		{
			if (level.getBlockEntity(pos) instanceof TubeBlockEntity tube)
			{
				tube.setNetwork(this);
			}
		}
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		else if (other instanceof RoutingNetwork)
		{
			RoutingNetwork otherNetwork = (RoutingNetwork)other;
			return this.endpoints.equals(otherNetwork.endpoints) && this.tubes.equals(otherNetwork.tubes);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return this.endpoints.hashCode() ^ this.tubes.hashCode();
	}
	
	@Override
	public String toString()
	{
		String endpointText = this.endpoints.stream().map(endpoint -> endpoint.toString()).reduce("Endpoints:\n", (head, tail) -> head + tail + "\n");
		String tubeText = this.tubes.stream().map(tube -> tube.toString()).reduce("Tubes:\n", (head, tail) -> head + tail + "\n");
		return endpointText + "\n" + tubeText;
	}
}
