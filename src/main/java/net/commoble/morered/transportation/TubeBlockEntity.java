package net.commoble.morered.transportation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.commoble.morered.routing.Route;
import net.commoble.morered.routing.RoutingNetwork;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public class TubeBlockEntity extends BlockEntity
{
	public static final String INV_NBT_KEY_ADD = "inventory_new_items";
	public static final String INV_NBT_KEY_RESET = "inventory_reset";
	public static final String CONNECTIONS = "connections";
	
	public static final AABB EMPTY_AABB = new AABB(0,0,0,0,0,0);
	
	public static final String SIDE = "side";
	
	public static final BlockEntityTicker<TubeBlockEntity> TICKER = (level,pos,state,tube) -> tube.tick();

	private Map<Direction, RemoteConnection> remoteConnections = new HashMap<>();
	private boolean isConnectionSyncDirty = false; // if true, sync to clients in update packet (always sync in update tag)
	
	public AABB renderAABB = EMPTY_AABB; // used by client, updated whenever NBT is read
	
	@Nonnull
	public Queue<ItemInTubeWrapper> inventory = new LinkedList<ItemInTubeWrapper>();
	
	protected final TubeInventoryHandler[] inventoryHandlers = Arrays.stream(Direction.values())
			.map(dir -> new TubeInventoryHandler(this, dir))
			.toArray(TubeInventoryHandler[]::new);	// one handler for each direction
	
	private Queue<ItemInTubeWrapper> wrappersToSendToClient = new LinkedList<ItemInTubeWrapper>();
	public Queue<ItemInTubeWrapper> incomingWrapperBuffer = new LinkedList<ItemInTubeWrapper>();
	
	@Nonnull	// use getNetwork()
	private RoutingNetwork network = RoutingNetwork.INVALID_NETWORK;

	public TubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public TubeBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().tubeEntity.get(), pos, state);
	}

	// connects two tube TEs
	// returns whether the attempt to add a connection was successful
	public static boolean addConnection(LevelAccessor level, Direction sideA, BlockPos posA, Direction sideB, BlockPos posB)
	{
		// if two tube TEs exist at the given locations, connect them and return true
		// otherwise return false
		return level.getBlockEntity(posA) instanceof TubeBlockEntity tubeA && level.getBlockEntity(posB) instanceof TubeBlockEntity tubeB
			? addConnection(level, tubeA, sideA, tubeB, sideB)
			: false;
	}

	// returns true if attempt to add a connection was successful
	public static boolean addConnection(LevelAccessor level, @Nonnull TubeBlockEntity fromTube, @Nonnull Direction fromSide, @Nonnull TubeBlockEntity toTube, @Nonnull Direction toSide)
	{
		fromTube.addConnection(fromSide, toSide, toTube.worldPosition, true);
		toTube.addConnection(toSide, fromSide, fromTube.worldPosition, false);
		return true;
	}

	// returns true if tube TEs exist at the given locations and both have a
	// connection to the other
	public static boolean areTubesConnected(LevelAccessor level, BlockPos posA, BlockPos posB)
	{
		return level.getBlockEntity(posA) instanceof TubeBlockEntity tubeA
			&& level.getBlockEntity(posB) instanceof TubeBlockEntity tubeB
			&& tubeA.hasRemoteConnection(posB)
			&& tubeB.hasRemoteConnection(posA);
	}

	// removes any connection between two tubes to each other
	// if only one tubes exists for some reason, or only one tube has a
	// connection to the other,
	// it will still attempt to remove its connection
	public static void removeConnection(LevelAccessor level, BlockPos posA, BlockPos posB)
	{
		if (level.getBlockEntity(posA) instanceof TubeBlockEntity tubeA)
			tubeA.removeConnection(posB);
		if (level.getBlockEntity(posB) instanceof TubeBlockEntity tubeB)
			tubeB.removeConnection(posA);
	}
	
	public static AABB getAABBContainingConnectedPositions(BlockPos startPos, Set<BlockPos> remoteConnections)
	{
		// make an AABB containing
		// -- the start pos
		// -- the blocks in the given set
		// -- also the six blocks adjacent to the start pos (so tubes can render adjacent outputs correctly
		AABB aabb = new AABB(startPos);
		for (BlockPos pos : remoteConnections)
		{
			aabb = aabb.minmax(new AABB(pos));
		}
		aabb = aabb.minmax(new AABB(startPos.offset(-1, -1, -1)));
		aabb = aabb.minmax(new AABB(startPos.offset(1,1,1)));
		return aabb;
	}
	
	/**** Getters and Setters	****/
	public RoutingNetwork getNetwork()
	{
		if (this.network.invalid)
		{
			this.network = RoutingNetwork.buildNetworkFrom(this.worldPosition, this.level);
		}
		return this.network;
	}
	
	public void setNetwork(RoutingNetwork network)
	{
		this.network = network;
	}
	
	public boolean isTubeCompatible (TubeBlockEntity tube)
	{
		Block thisBlock = this.getBlockState().getBlock();
		Block otherBlock = tube.getBlockState().getBlock();
		if (thisBlock instanceof TubeBlock && otherBlock instanceof TubeBlock)
		{
			return ((TubeBlock)thisBlock).isTubeCompatible((TubeBlock)otherBlock);
		}
		return false;
	}
	
	public Set<Direction> getAdjacentConnectedDirections()
	{
		BlockState state = this.getBlockState();
		return TubeBlock.getConnectedDirections(state);
	}
	
	public Set<Direction> getAllConnectedDirections()
	{
		Set<Direction> result = new HashSet<>();
		result.addAll(this.getAdjacentConnectedDirections());
		result.addAll(this.getRemoteConnections().keySet());
		return result;
	}
	
	// insertionSide is the side of this block the item was inserted from
	public Route getBestRoute(Direction insertionSide, ItemStack stack)
	{
		return this.getNetwork().getBestRoute(this.level, this.worldPosition, insertionSide, stack);
	}
	
	public Map<Direction, RemoteConnection> getRemoteConnections()
	{
		return this.remoteConnections;
	}
	
	public boolean hasRemoteConnection(BlockPos otherPos)
	{
		return this.getDirectionOfRemoteConnection(otherPos) != null;
	}
	
	public @Nullable RemoteConnection getRemoteConnection(Direction face)
	{
		return this.getRemoteConnections().get(face);
	}
	
	/**
	 * 
	 * @param face The face of the tube block
	 * @return TRUE if the tile entity has a remote connection from the given side
	 */
	public boolean hasRemoteConnection(Direction face)
	{
		return this.getRemoteConnections().containsKey(face);
	}

	/**
	 * 
	 * @param otherPos
	 * @return The side of this tube that has a connection to the given position, if any
	 */
	public @Nullable Direction getDirectionOfRemoteConnection(BlockPos otherPos)
	{
		for (var entry : this.remoteConnections.entrySet())
		{
			if (entry.getValue().toPos.equals(otherPos))
				return entry.getKey();
		}
		return null;
	}

	public void clearRemoteConnections()
	{
		if (!this.remoteConnections.isEmpty())
		{
			for (var entry : this.remoteConnections.entrySet())
			{
				if (this.level.getBlockEntity(entry.getValue().toPos) instanceof TubeBlockEntity otherTube)
				{
					otherTube.removeConnection(this.worldPosition);
				}
			}
			this.remoteConnections = new HashMap<>();
			this.setConnectionSyncDirty(true);
			this.onDataUpdated();
		}
	}

	private void addConnection(Direction thisSide, Direction otherSide, BlockPos otherPos, boolean isPrimary)
	{
		this.remoteConnections.put(thisSide, new RemoteConnection(thisSide, otherSide, this.worldPosition, otherPos, isPrimary));
		this.network.invalid = true;
		this.setConnectionSyncDirty(true);
		this.onDataUpdated();
	}

	private void removeConnection(BlockPos otherPos)
	{
		BlockState newState = this.getBlockState();
		for (Direction dir : Direction.values())
		{
			RemoteConnection connection = this.remoteConnections.get(dir);
			if (connection != null && connection.toPos.equals(otherPos))
			{
				this.remoteConnections.remove(dir);
				this.setConnectionSyncDirty(true);
			}
		}
		if (this.level instanceof ServerLevel serverLevel)
		{
			this.onPossibleNetworkUpdateRequired();
			this.network.invalid = true;
			this.level.setBlockAndUpdate(this.worldPosition, newState);
			PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new TubeBreakPacket(Vec3.atCenterOf(this.worldPosition), Vec3.atCenterOf(otherPos)));
		}
		this.onDataUpdated();
	}
	
	public boolean isConnectionSyncDirty()
	{
		return this.isConnectionSyncDirty;
	}
	
	public void setConnectionSyncDirty(boolean dirty)
	{
		this.isConnectionSyncDirty = dirty;
	}

	/**** Event Handling ****/
	
	public void onPossibleNetworkUpdateRequired()
	{
		if (!this.network.invalid && this.didNetworkChange())
		{
			this.network.invalid = true;
		}
	}
	
	private boolean didNetworkChange()
	{
		for (Direction face : Direction.values())
		{
			BlockPos checkPos = this.worldPosition.relative(face);
			// if the adjacent block is a tube or endpoint but isn't in the network
			// OR if the adjacent block is in the network but isn't a tube or endpoint
			// then the network changed
			if (this.getNetwork().contains(this.worldPosition, face.getOpposite()) != this.getNetwork().isValidToBeInNetwork(checkPos, this.level, face.getOpposite()))
				return true;
		}
		return false;
	}
	
	protected void tick()
	{
		this.mergeBuffer();
		if (!this.inventory.isEmpty())	// if inventory is empty, skip the tick
		{
			if (!this.level.isClientSide)	// block has changes that need to be saved (serverside)
			{
				this.setChanged();
			}
			Queue<ItemInTubeWrapper> remainingWrappers = new LinkedList<ItemInTubeWrapper>();
			for (ItemInTubeWrapper wrapper : this.inventory)
			{
				wrapper.ticksElapsed++;
				if (wrapper.ticksElapsed >= wrapper.maximumDurationInTube)
				{
					if (wrapper.freshlyInserted)
					{
						wrapper.freshlyInserted = false;
						wrapper.remainingMoves.removeFirst();
						wrapper.ticksElapsed = 0;
						remainingWrappers.add(wrapper);
					}
					else
					{
						this.sendWrapperOnward(wrapper);
					}
				}
				else
				{
					remainingWrappers.add(wrapper);
				}
			}
			this.inventory = remainingWrappers;
		}
		if (!this.level.isClientSide && this.inventory.size() > MoreRed.SERVERCONFIG.maxItemsInTube().get())
		{
			this.level.removeBlock(this.worldPosition, false);
		}
	}
	
	public BlockPos getConnectedPos(Direction dir)
	{
		if (this.remoteConnections.containsKey(dir))
		{
			return this.remoteConnections.get(dir).toPos;
		}
		else
		{
			return this.worldPosition.relative(dir);
		}
	}
	
	public void sendWrapperOnward(ItemInTubeWrapper wrapper)
	{
		if (!wrapper.remainingMoves.isEmpty())	// wrapper has remaining moves
		{
			Direction dir = wrapper.remainingMoves.poll();
			BlockPos nextPos = this.getConnectedPos(dir);
			BlockEntity nextBlockEntity = this.level.getBlockEntity(nextPos);
			if (nextBlockEntity instanceof TubeBlockEntity nextTube) // te exists and is a valid tube
			{
				if (this.isTubeCompatible(nextTube) || this.hasRemoteConnection(nextPos))
				{
					(nextTube).enqueueItemStack(wrapper.stack, wrapper.remainingMoves, wrapper.maximumDurationInTube);
				}
			}
			else if (!this.level.isClientSide)
			{
				IItemHandler nextHandler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, nextPos, dir.getOpposite());
				if (nextHandler != null)	// te exists but is not a tube
				{
					ItemStack remaining = WorldHelper.disperseItemToHandler(wrapper.stack, nextHandler);
						
					if (!remaining.isEmpty())	// target inventory filled up unexpectedly
					{
						ItemStack unenqueueableItems = this.enqueueItemStack(remaining, dir, false); // attempt to re-enqueue the item on that side
						WorldHelper.ejectItemstack(this.level, this.worldPosition, dir, unenqueueableItems);	// eject items if we can't
					}
				}
				else	// no TE -- eject stack
				{
					WorldHelper.ejectItemstack(this.level, this.worldPosition, dir, wrapper.stack);
				}
			}
		}
		else if (!this.level.isClientSide)	// wrapper has no remaining moves -- this isn't expected, eject the item
		{
			WorldHelper.ejectItemstack(this.level, this.worldPosition, null, wrapper.stack);
		}
	}

	public void onDataUpdated()
	{
		this.setChanged();
		this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
	}
	
	/**** Inventory handling ****/

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side)
	{
		return side == null ? null : this.inventoryHandlers[side.ordinal()];
	}

	// insert a new itemstack into the tube network from a direction
	// and determine a route for it
	public ItemStack enqueueItemStack(ItemStack stack, Direction face, boolean simulate)
	{
		Route route = this.getNetwork().getBestRoute(this.level, this.worldPosition, face, stack);
		if (route == null || route.sequenceOfMoves.isEmpty())
			return stack.copy();
			
		if (simulate)
			return ItemStack.EMPTY;
		
		int ticks_per_tube = this.getNetwork().getTicksPerTube();
		this.wrappersToSendToClient.add(new ItemInTubeWrapper(stack, route.sequenceOfMoves, ticks_per_tube, face.getOpposite()));

		this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
		
		return this.enqueueItemStack(new ItemInTubeWrapper(stack, route.sequenceOfMoves, 10, face.getOpposite()));
	}
	
	public ItemStack enqueueItemStack(ItemInTubeWrapper wrapper)
	{
		this.incomingWrapperBuffer.add(wrapper);
		return ItemStack.EMPTY;
	}

	public ItemStack enqueueItemStack(ItemStack stack, Queue<Direction> remainingMoves, int ticksPerTube)
	{
		return this.enqueueItemStack(new ItemInTubeWrapper(stack, remainingMoves, ticksPerTube));
	}
	
	public void mergeBuffer()
	{
		if (!this.incomingWrapperBuffer.isEmpty())
		{
			for (ItemInTubeWrapper wrapper : this.incomingWrapperBuffer)
			{
				this.inventory.add(wrapper);
			}
			this.incomingWrapperBuffer = new LinkedList<ItemInTubeWrapper>();
		}
	}

	public void dropItems()
	{
		this.mergeBuffer();
		for (ItemInTubeWrapper wrapper : this.inventory)
		{
			Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), wrapper.stack);
		}

		this.inventory = new LinkedList<ItemInTubeWrapper>();	// clear it in case this is being called without destroying the TE
	}

	public static boolean isSpaceForAnythingInItemHandler(IItemHandler handler)
	{
		return true;
	}

	public boolean isAnyInventoryAdjacent()
	{
		for (Direction face : Direction.values())
		{
			BlockPos neighborPos = this.worldPosition.relative(face);
			BlockEntity te = this.level.getBlockEntity(neighborPos);
			if (!(te instanceof TubeBlockEntity))
			{
				IItemHandler handler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, face.getOpposite());
				// if a nearby inventory that is not a tube exists
				if (handler != null)
				{
					if (TubeBlockEntity.isSpaceForAnythingInItemHandler(handler))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	/** read **/
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries); // reads neoforge data and third-party capabilities
		this.readAllNBT(compound, registries);
	}

	@Override	// write entire inventory by default (for server -> hard disk purposes this is what is called)
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries); // saves neoforge data and third-party capabilities
		this.writeAllNBT(compound, registries);
	}
	
	// write all nbt specific to tubes
	protected void writeAllNBT(CompoundTag compound, HolderLookup.Provider registries)
	{
		BlockPos tubePos = this.getBlockPos();
		BlockState state = this.getBlockState();
		OctahedralGroup group = state.getValue(TubeBlock.GROUP);
		OctahedralGroup normalizer = group.inverse();
		ListTag invList = new ListTag();
		this.mergeBuffer();

		for (ItemInTubeWrapper wrapper : this.inventory)
		{
			CompoundTag invTag = new CompoundTag();
			wrapper.writeToNBT(invTag, group, registries);
			invList.add(invTag);
		}
		if (!invList.isEmpty())
		{
			compound.put(INV_NBT_KEY_RESET, invList);
		}
		
		CompoundTag connectionsTag = new CompoundTag();
		this.remoteConnections.forEach((direction, connection) ->
		{
			connectionsTag.put(normalizer.rotate(direction).getName(), connection.toStorage(tubePos).toNBT(group));
		});
		compound.put(CONNECTIONS, connectionsTag);
	}
	
	// read all nbt specific to tubes
	protected void readAllNBT(@Nullable CompoundTag compound, HolderLookup.Provider registries)
	{
		// ensures stuff gets synced when we load from data in mid-session
		// (e.g. from a structure block)
		this.setConnectionSyncDirty(true);
		
		if (compound != null) // netcode sends empty compounds as null
		{
			BlockState state = this.getBlockState();
			OctahedralGroup group = state.getValue(TubeBlock.GROUP);
			if (compound.contains(INV_NBT_KEY_RESET))	// only update inventory if the compound has an inv. key
			{									// this lets the client receive packets without the inventory being cleared
				ListTag invList = compound.getList(INV_NBT_KEY_RESET, 10);
				Queue<ItemInTubeWrapper> inventory = new LinkedList<ItemInTubeWrapper>();
				for (int i = 0; i < invList.size(); i++)
				{
					CompoundTag itemTag = invList.getCompound(i);
					inventory.add(ItemInTubeWrapper.readFromNBT(itemTag, group, registries));
				}
				this.inventory = inventory;
			}
			else if (compound.contains(INV_NBT_KEY_ADD))	// add newly inserted items to this tube
			{
				ListTag invList = compound.getList(INV_NBT_KEY_ADD, 10);
				for (int i=0; i<invList.size(); i++)
				{
					CompoundTag itemTag = invList.getCompound(i);
					this.inventory.add(ItemInTubeWrapper.readFromNBT(itemTag, group, registries));
				}
			}
			

			if (compound.contains(CONNECTIONS))
			{
				CompoundTag connectionsTag = compound.getCompound(CONNECTIONS);
				Map<Direction, RemoteConnection> newMap = new HashMap<>();
				Direction[] dirs = Direction.values();
				for (int i=0; i<6; i++)
				{
					Direction dir = dirs[i];
					String dirName = dir.getName();
					if (connectionsTag.contains(dirName))
					{
						CompoundTag connectionTag = connectionsTag.getCompound(dirName);
						RemoteConnection.Storage storage = RemoteConnection.Storage.fromNBT(connectionTag, group);
						RemoteConnection connection = RemoteConnection.fromStorage(storage, dir, this.worldPosition);
						newMap.put(group.rotate(dir), connection);
					}
					
				}
				this.remoteConnections = newMap;
			}
		}
		this.renderAABB = getAABBContainingConnectedPositions(this.worldPosition,
			this.remoteConnections.values().stream()
				.map(connection -> connection.toPos)
				.collect(Collectors.toSet()));
	}

	/**
	 * Get an NBT compound to sync to the client with SPacketChunkData, used for
	 * initial loading of the chunk or when many blocks change at once. This
	 * compound comes back to you clientside in {@link handleUpdateTag}
	 * //handleUpdateTag just calls read by default
	 */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag compound = super.getUpdateTag(registries);
		this.setConnectionSyncDirty(true);
		this.writeAllNBT(compound, registries);	// okay to send entire inventory on chunk load
		this.setConnectionSyncDirty(false);
		return compound;
	}	

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
	{
		this.readAllNBT(tag, registries);
	}

	/**
	 * Prepare a packet to sync TE to client
	 * We don't need to send the inventory in every packet
	 * but we should notify the client of new items entering the network
	 */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this, (be,registries) -> this.makeUpdatePacketTag(registries));
	}
	
	protected CompoundTag makeUpdatePacketTag(HolderLookup.Provider registries)
	{
		BlockState state = this.getBlockState();
		BlockPos pos = this.getBlockPos();
		OctahedralGroup group = state.getValue(TubeBlock.GROUP);
		OctahedralGroup normalizer = group.inverse();
		
		CompoundTag compound = new CompoundTag();
		super.saveAdditional(compound, registries); // write the basic TE stuff

		ListTag invList = new ListTag();

		while (!this.wrappersToSendToClient.isEmpty())
		{
			// empty itemstacks are not added to the tube
			ItemInTubeWrapper wrapper = this.wrappersToSendToClient.poll();
			CompoundTag invTag = new CompoundTag();
			wrapper.writeToNBT(invTag, group, registries);
			invList.add(invTag);
		}
		if (!invList.isEmpty())
		{
			compound.put(INV_NBT_KEY_ADD, invList);
		}
		
		if (this.isConnectionSyncDirty())
		{
			CompoundTag connectionsTag = new CompoundTag();
			this.remoteConnections.forEach((direction, connection) ->
			{
				connectionsTag.put(normalizer.rotate(direction).getName(), connection.toStorage(pos).toNBT(group));
			});
			compound.put(CONNECTIONS, connectionsTag);
			this.setConnectionSyncDirty(false);
		}
		
		
		return compound;
	}

	/**
	 * Receive packet on client and get data out of it
	 */
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries)
	{
		this.readAllNBT(packet.getTag(), registries);
	}
	
	public void setConnectionsRaw(Map<Direction, RemoteConnection> newConnections)
	{
		this.remoteConnections = newConnections;
	}
}
