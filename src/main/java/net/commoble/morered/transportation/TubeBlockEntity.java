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

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.commoble.morered.routing.Route;
import net.commoble.morered.routing.RoutingNetwork;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueOutput.ValueOutputList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class TubeBlockEntity extends BlockEntity
{
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String INV_NBT_KEY_ADD = "inventory_new_items";
	public static final String INV_NBT_KEY_RESET = "inventory";
	public static final String CONNECTIONS = "connections";
	
	public static final AABB EMPTY_AABB = new AABB(0,0,0,0,0,0);
	
	public static final String SIDE = "side";
	
	public static final BlockEntityTicker<TubeBlockEntity> TICKER = (level,pos,state,tube) -> tube.tick();

	private Map<Direction, RemoteConnection> remoteConnections = new HashMap<>();
	private boolean isConnectionSyncDirty = false; // if true, sync to clients in update packet (always sync in update tag)
	
	public AABB renderAABB = EMPTY_AABB; // used by client, updated whenever NBT is read
	private Map<Direction, TubeConnectionRenderInfo> connectionRenderInfos = null;
	
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
		this(MoreRed.TUBE_BLOCK_ENTITY.get(), pos, state);
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
	public Route getBestRoute(Direction insertionSide, ItemStack stack, TransactionContext context)
	{
		return this.getNetwork().getBestRoute(this.level, this.worldPosition, insertionSide, stack, context);
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
			if (!this.level.isClientSide())	// block has changes that need to be saved (serverside)
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
		if (!this.level.isClientSide() && this.inventory.size() > MoreRed.SERVERCONFIG.maxItemsInTube().get())
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
			else if (!this.level.isClientSide())
			{
				ResourceHandler<ItemResource> nextHandler = this.level.getCapability(Capabilities.Item.BLOCK, nextPos, dir.getOpposite());
				if (nextHandler != null)	// te exists but is not a tube
				{
					ItemStack stackInWrapper = wrapper.stack;
					int toInsert = stackInWrapper.getCount();
					int inserted = ResourceHandlerUtil.insertStacking(nextHandler, ItemResource.of(stackInWrapper), toInsert, null);
					int remaining = toInsert = inserted;
					if (remaining > 0)	// target inventory filled up unexpectedly
					{
						ItemStack remainingStack = stackInWrapper.copyWithCount(remaining);
						@Nullable Route nextRoute = this.getBestRoute(dir, remainingStack, Transaction.openRoot());
						if (nextRoute == null)
						{
							// if we can't reenqueue item, just eject them
							WorldHelper.ejectItemstack(this.level, this.worldPosition, dir, remainingStack);
						}
						else
						{
							// if we can reenqueue item, do so
							this.enqueueItemStack(remainingStack, dir, nextRoute);
						}
					}
				}
				else	// no TE -- eject stack
				{
					WorldHelper.ejectItemstack(this.level, this.worldPosition, dir, wrapper.stack);
				}
			}
		}
		else if (!this.level.isClientSide())	// wrapper has no remaining moves -- this isn't expected, eject the item
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
	public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side)
	{
		return side == null ? null : this.inventoryHandlers[side.ordinal()];
	}
	
	public @Nullable Route getRouteForStack(ItemStack stack, Direction face, TransactionContext context)
	{
		return this.getNetwork().getBestRoute(this.level, this.worldPosition, face, stack, context);
	}
		
	public void enqueueItemStack(ItemStack stack, Direction face, Route route)
	{
		int ticks_per_tube = this.getNetwork().getTicksPerTube();
		this.wrappersToSendToClient.add(new ItemInTubeWrapper(stack, route.sequenceOfMoves, ticks_per_tube, face.getOpposite()));

		this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
		
		this.enqueueItemStack(new ItemInTubeWrapper(stack, route.sequenceOfMoves, 10, face.getOpposite()));
	}
	
	public void enqueueItemStack(ItemInTubeWrapper wrapper)
	{
		this.incomingWrapperBuffer.add(wrapper);
	}

	public void enqueueItemStack(ItemStack stack, Queue<Direction> remainingMoves, int ticksPerTube)
	{
		this.enqueueItemStack(new ItemInTubeWrapper(stack, remainingMoves, ticksPerTube));
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

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState newState)
	{
		super.preRemoveSideEffects(pos, newState);
		if (this.level instanceof ServerLevel serverLevel)
		{
			TubesInChunk.updateTubeSet(serverLevel, pos, Set<BlockPos>::remove);
			this.mergeBuffer();
			for (ItemInTubeWrapper wrapper : this.inventory)
			{
				Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), wrapper.stack);
			}
			this.clearRemoteConnections();
		}
	}

	@Override
	/** read **/
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input); // reads neoforge data and third-party capabilities
		// ensures stuff gets synced when we load from data in mid-session
		// (e.g. from a structure block)
		this.setConnectionSyncDirty(true);
		
		BlockState state = this.getBlockState();
		OctahedralGroup group = state.getValue(TubeBlock.GROUP);
		
		input.childrenList(INV_NBT_KEY_RESET).ifPresentOrElse(list -> {
			// only update inventory if the compound has an inv. key
			// this lets the client receive packets without the inventory being cleared
			Queue<ItemInTubeWrapper> inventory = new LinkedList<>();
			list.stream().forEach(itemInTubeInput -> inventory.add(ItemInTubeWrapper.readFromNBT(itemInTubeInput, group)));
			this.inventory = inventory;
		}, () -> input.childrenList(INV_NBT_KEY_ADD).ifPresent(list -> {
			// if we are not resetting inventory,
			// add newly inserted items to this tube
			list.stream().forEach(itemInTubeInput -> this.inventory.add(ItemInTubeWrapper.readFromNBT(itemInTubeInput, group)));
		}));
		
		input.childrenList(INV_NBT_KEY_ADD).ifPresent(list -> {
		});
		input.childrenList(INV_NBT_KEY_RESET).ifPresent(list -> {
		});
	
		input.child(CONNECTIONS).ifPresent(connectionsTag -> {
			Map<Direction, RemoteConnection> newMap = new HashMap<>();
			Direction[] dirs = Direction.values();
			for (int i=0; i<6; i++)
			{
				Direction dir = dirs[i];
				String dirName = dir.getName();
				connectionsTag.child(dirName).ifPresent(connectionTag -> {
					RemoteConnection.Storage storage = RemoteConnection.Storage.fromNBT(connectionTag, group);
					RemoteConnection connection = RemoteConnection.fromStorage(storage, dir, this.worldPosition);
					newMap.put(group.rotate(dir), connection);
				});
			}
			this.remoteConnections = newMap;
		});

		this.renderAABB = getAABBContainingConnectedPositions(this.worldPosition,
			this.remoteConnections.values().stream()
				.map(connection -> connection.toPos)
				.collect(Collectors.toSet()));
		this.connectionRenderInfos = null;
	}

	@Override	// write entire inventory by default (for server -> hard disk purposes this is what is called)
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output); // saves neoforge data and third-party capabilities
		BlockPos tubePos = this.getBlockPos();
		BlockState state = this.getBlockState();
		OctahedralGroup group = state.getValue(TubeBlock.GROUP);
		OctahedralGroup normalizer = group.inverse();
		this.mergeBuffer();

		if (!this.inventory.isEmpty())
		{
			ValueOutputList outputList = output.childrenList(INV_NBT_KEY_RESET);
			for (ItemInTubeWrapper wrapper : this.inventory)
			{
				wrapper.writeToNBT(outputList.addChild(), group);
			}
		}
		
		ValueOutput connectionsTag = output.child(CONNECTIONS);
		this.remoteConnections.forEach((direction, connection) ->
		{
			connection.toStorage(tubePos).toNBT(connectionsTag.child(normalizer.rotate(direction).getName()), group);
		});
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
		this.setConnectionSyncDirty(true);
		CompoundTag tag = this.saveCustomOnly(registries);
		this.setConnectionSyncDirty(false);
		return tag;
	}
	
	// super.handleUpdateTag invokes loadAdditional

	/**
	 * Prepare a packet to sync TE to client
	 * We don't need to send the inventory in every packet
	 * but we should notify the client of new items entering the network
	 */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this, (be,registries) -> {
			CompoundTag tag;
			try (ProblemReporter.ScopedCollector problemCollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER))
			{
				TagValueOutput tagOutput = TagValueOutput.createWithContext(problemCollector, registries);
				// think there was an old bug here where we were calling saveAdditional incorrectly
				// (since 1.16.4 or older!)
				// see if the tube works without this and then remove it later
//				this.saveCustomOnly(tagOutput);
				this.writeToUpdatePacket(tagOutput);
				tag = tagOutput.buildResult();
			}
			return tag;
		});
	}
	
	protected void writeToUpdatePacket(ValueOutput output)
	{
		BlockState state = this.getBlockState();
		BlockPos pos = this.getBlockPos();
		OctahedralGroup group = state.getValue(TubeBlock.GROUP);
		OctahedralGroup normalizer = group.inverse();
		
		if (!this.wrappersToSendToClient.isEmpty())
		{
			ValueOutputList outputList = output.childrenList(INV_NBT_KEY_ADD);
			// empty itemstacks will not have been added to the tube

			while (!this.wrappersToSendToClient.isEmpty())
			{
				ItemInTubeWrapper wrapper = this.wrappersToSendToClient.poll();
				wrapper.writeToNBT(outputList.addChild(), group);
			}
		}
		
		if (this.isConnectionSyncDirty())
		{
			ValueOutput connectionsTag = output.child(CONNECTIONS);
			this.remoteConnections.forEach((direction, connection) ->
			{
				connection.toStorage(pos).toNBT(
					connectionsTag.child(normalizer.rotate(direction).getName()),
					group);
			});
			this.setConnectionSyncDirty(false);
		}
	}

	// super.onDataPacket invokes loadAdditional
	
	public void setConnectionsRaw(Map<Direction, RemoteConnection> newConnections)
	{
		this.remoteConnections = newConnections;
	}

	public static class TubeConnectionRenderInfo
	{
		public final BlockPos endPos;
		public final Direction endFace;
		public int endLight = 0;
		
		public TubeConnectionRenderInfo(BlockPos endPos, Direction endFace)
		{
			this.endPos = endPos;
			this.endFace = endFace;
		}

		public void update(Level level)
		{
			int blockLight = level.getBrightness(LightLayer.BLOCK, this.endPos);
			int skyLight = level.getBrightness(LightLayer.SKY, this.endPos);
			this.endLight = LightTexture.pack(blockLight, skyLight);
		}
	}
	
	public Map<Direction, TubeConnectionRenderInfo> getConnectionRenderInfos()
	{
		Map<Direction, TubeConnectionRenderInfo> results = this.connectionRenderInfos;
		if (results == null)
		{
			results = new HashMap<>();
			for (var entry : this.remoteConnections.entrySet())
			{
				RemoteConnection connection = entry.getValue();
				if (connection.isPrimary)
				{
					results.put(entry.getKey(), new TubeConnectionRenderInfo(connection.toPos, connection.toSide));	
				}
			}
			
			this.connectionRenderInfos = results;
		}
		
		return results;
	}
}
