package net.commoble.morered.wires;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.math.OctahedralGroup;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.ExMachinaGameEvents;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.SignalStrength;
import net.commoble.exmachina.api.StateWirer;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.MoreRed;
import net.commoble.morered.util.DirectionHelper;
import net.commoble.morered.util.EightGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;

public abstract class AbstractWireBlock extends Block implements FaceSegmentBlock
{
	
	public static final BooleanProperty DOWN = PipeBlock.DOWN;
	public static final BooleanProperty UP = PipeBlock.UP;
	public static final BooleanProperty NORTH = PipeBlock.NORTH;
	public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
	public static final BooleanProperty WEST = PipeBlock.WEST;
	public static final BooleanProperty EAST = PipeBlock.EAST;
	public static final BooleanProperty[] INTERIOR_FACES = {DOWN,UP,NORTH,SOUTH,WEST,EAST};
	public static final EnumProperty<OctahedralGroup> TRANSFORM = EightGroup.TRANSFORM;
	
	/**
	 * 
	 * @param nodeShapes array of 6 voxelshapes by face direction
	 * @param lineShapes array of 24 voxelshapes by face direction + secondary direction
	 * @return An array of 64 voxelshapes for wire nodes and elbows (the shapes that can be determined just from blockstate combinations and no worldpos context)
	 */
	public static VoxelShape[] makeVoxelShapes(VoxelShape[] nodeShapes, VoxelShape[] lineShapes)
	{
		VoxelShape[] result = new VoxelShape[64];
		
		for (int i=0; i<64; i++)
		{
			VoxelShape nextShape = Shapes.empty();
			boolean[] addedSides = new boolean[6]; // sides for which we've already added node shapes to
			for (int side=0; side<6; side++)
			{
				if ((i & (1 << side)) != 0)
				{
					nextShape = Shapes.or(nextShape, nodeShapes[side]);
					
					int sideAxis = side/2; // 0,1,2 = y,z,x
					for (int secondarySide = 0; secondarySide < side; secondarySide++)
					{
						if (addedSides[secondarySide] && sideAxis != secondarySide/2) // the two sides are orthagonal to each other (not parallel)
						{
							// add line shapes for the elbows
							nextShape = Shapes.or(nextShape, getLineShape(lineShapes, side, DirectionHelper.getCompressedSecondSide(side,secondarySide)));
							nextShape = Shapes.or(nextShape, getLineShape(lineShapes, secondarySide, DirectionHelper.getCompressedSecondSide(secondarySide,side)));
						}
					}
					
					addedSides[side] = true;
				}
			}
			result[i] = nextShape;
		}
		
		return result;
	}
	
	public static VoxelShape getLineShape(VoxelShape[] lineShapes, int side, int secondarySide)
	{
		return lineShapes[side*4 + secondarySide];
	}
	
	/**
	 * Get the index of the primary shape for the given wireblock blockstate (64 combinations)
	 * @param state A blockstate belonging to an AbstractWireBlock
	 * @return An index in the range [0, 63]
	 */
	public static int getShapeIndex(BlockState state)
	{
		int index = 0;
		int sideCount = INTERIOR_FACES.length;
		for (int side=0; side < sideCount; side++)
		{
			if (state.getValue(INTERIOR_FACES[side]))
			{
				index |= 1 << side;
			}
		}
		
		return index;
	}
	
	public static VoxelShape makeExpandedShapeForIndex(VoxelShape[] shapesByStateIndex, VoxelShape[] lineShapes, long index)
	{
		int primaryShapeIndex = (int) (index & 63);
		long expandedShapeIndex = index >> 6;
		VoxelShape shape = shapesByStateIndex[primaryShapeIndex];
		// we want to use the index to combine secondary line shapes with the actual voxelshape for a given state
		int flag = 1;
		for (int side = 0; side < 6; side++)
		{
			for (int subSide = 0; subSide < 4; subSide++)
			{
				if ((expandedShapeIndex & flag) != 0)
				{
					shape = Shapes.or(shape, AbstractWireBlock.getLineShape(lineShapes, side, subSide));
				}
				flag = flag << 1;
			}
		}
		return shape;
	}
	
	public static LoadingCache<Long, VoxelShape> makeVoxelCache(VoxelShape[] shapesByStateIndex, VoxelShape[] lineShapes)
	{
		return CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.build(new CacheLoader<Long, VoxelShape>()
			{
	
				@Override
				public VoxelShape load(Long key) throws Exception
				{
					return AbstractWireBlock.makeExpandedShapeForIndex(shapesByStateIndex, lineShapes, key);
				}
			
			});
	}

	protected final VoxelShape[] shapesByStateIndex;
	protected final Map<Direction, VoxelShape> raytraceBackboards;
	protected final LoadingCache<Long, VoxelShape> voxelCache;
	protected final ChannelSet channels;
	protected final boolean readAttachedPower;
	protected final boolean notifyAttachedNeighbors;
	protected final boolean useIndirectPower;

	/**
	 * 
	 * @param properties block properties
	 * @param shapesByStateIndex Array of 64 voxelshapes, the voxels for the canonical blockstates
	 * @param raytraceBackboards Array of 6 voxelshapes (by attachment face direction) used for subwire raytrace checking
	 * @param voxelCache The cache to use for this block's voxels given world context
	 * @param useIndirectPower Whether this block is allowed to send or receive power conducted indirectly through solid cubes
	 */
	public AbstractWireBlock(Properties properties, VoxelShape[] shapesByStateIndex, Map<Direction,VoxelShape> raytraceBackboards, LoadingCache<Long, VoxelShape> voxelCache, boolean readAttachedPower, boolean notifyAttachedNeighbors, boolean useIndirectPower, ChannelSet channels)
	{
		super(properties);
		// the "default" state has to be the empty state so we can build it up one face at a time
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(DOWN, false)
			.setValue(UP, false)
			.setValue(NORTH, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false)
			.setValue(EAST, false)
			.setValue(TRANSFORM, OctahedralGroup.IDENTITY)
			);
		this.shapesByStateIndex = shapesByStateIndex;
		this.raytraceBackboards = raytraceBackboards;
		this.voxelCache = voxelCache;
		this.readAttachedPower = readAttachedPower;
		this.notifyAttachedNeighbors = notifyAttachedNeighbors;
		this.useIndirectPower = useIndirectPower;
		this.channels = channels;
	}

	protected abstract Map<Direction, SignalStrength> onReceivePower(LevelAccessor level, BlockPos pos, BlockState state, Direction attachmentSide, int power, Channel channel);
	
	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DOWN,UP,NORTH,SOUTH,WEST,EAST,TRANSFORM);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		return worldIn instanceof Level level
			? VoxelCache.get(level, pos)
			: this.shapesByStateIndex[getShapeIndex(state)];
	}

	// overriding this so we don't delegate to getShape for the render shape (to reduce lookups of the extended shape)
	@Override
	public VoxelShape getOcclusionShape(BlockState state)
	{
		return this.shapesByStateIndex[getShapeIndex(state)];
	}

	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
	{
		return this.isEmptyWireBlock(state);
	}

	@Override
	public BlockState updateShape(
		BlockState thisState,
		LevelReader world,
		ScheduledTickAccess ticker,
		BlockPos thisPos,
		Direction directionToNeighbor,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random)
	{
		BooleanProperty sideProperty = INTERIOR_FACES[directionToNeighbor.ordinal()];
		if (thisState.getValue(sideProperty)) // if wire is attached on the relevant face, check if it should be disattached
		{
			Direction neighborSide = directionToNeighbor.getOpposite();
			boolean isNeighborSideSolid = neighborState.isFaceSturdy(world, neighborPos, neighborSide);
			if (!isNeighborSideSolid && world instanceof ServerLevel)
			{
				Block.dropResources(this.defaultBlockState().setValue(sideProperty, true), (ServerLevel)world, thisPos);
			}
			return thisState.setValue(sideProperty, isNeighborSideSolid);
		}
		else
		{
			return thisState;
		}
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos)
	{
		Direction[] dirs = Direction.values();
		for (int side=0; side<6; side++)
		{
			if (state.getValue(INTERIOR_FACES[side]))
			{
				Direction dir = dirs[side];
				BlockPos neighborPos = pos.relative(dir);
				BlockState neighborState = world.getBlockState(neighborPos);
				Direction neighborSide = dir.getOpposite();
				if (!neighborState.isFaceSturdy(world, neighborPos, neighborSide))
				{
					return false;
				}
			}
		}
		return true;
	}

	// we can use the blockitem onItemUse to handle adding extra nodes to an existing wire
	// so for this, we can assume that we're placing a fresh wire
	/**
	 * @return null if no state should be placed, or the state that should be placed otherwise
	 */
	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Level world = context.getLevel();
		Direction sideOfNeighbor = context.getClickedFace();
		Direction directionToNeighbor = sideOfNeighbor.getOpposite();
		// with standard BlockItemUseContext rules,
		// if replacing a block, this is the position of the block we're replacing
		// otherwise, it's the position adjacent to the clicked block (offset by sideOfNeighbor)
		BlockPos placePos = context.getClickedPos();
		BlockPos neighborPos = placePos.relative(directionToNeighbor);
		BlockState neighborState = world.getBlockState(neighborPos);
		return neighborState.isFaceSturdy(world, neighborPos, sideOfNeighbor)
			? this.defaultBlockState().setValue(INTERIOR_FACES[directionToNeighbor.ordinal()], true)
			: null;
	}
	
	// called when a different block instance is replaced with this one
	// only called on server
	// called after previous block and TE are removed, but before this block's TE is added
	@Override
	public void onPlace(BlockState newState, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		super.onPlace(newState, worldIn, pos, oldState, isMoving);
		this.updateShapeCache(worldIn, pos);
		// if this is an empty wire block, remove it if no edges are valid anymore
		boolean doPowerUpdate = true;
		if (this.isEmptyWireBlock(newState)) // wire block has no attached faces and consists only of fake wire edges
		{
			// delay until next tick, because we can't destroy the block in onPlace (confuses the setBlock code)
			worldIn.scheduleTick(pos, this, 1);
			doPowerUpdate = false;
		}
		else if (worldIn.getBlockEntity(pos) instanceof WireBlockEntity wire)
		{
			wire.setConnectionsWithServerUpdate(this.getExpandedShapeIndex(newState, worldIn, pos));
		}
		// if the new state is still a wire block and has at least one wire in it, do a power update
		if (doPowerUpdate)
		{
			ExMachinaGameEvents.scheduleSignalGraphUpdate(worldIn, pos);
		}
	}
	
	
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		super.tick(state, level, pos, rand);
		long edgeFlags = this.getEdgeFlags(level,pos);
		if (edgeFlags == 0)	// if we don't need to be rendering any edges, set wire block to air
		{
			// removing the block will call onReplaced again, but using the newBlock != this condition
			level.removeBlock(pos, false);
		}
		else if (level.getBlockEntity(pos) instanceof WireBlockEntity wire)
		{
			wire.setConnectionsWithServerUpdate(this.getExpandedShapeIndex(state, level, pos));
		}
	}

	// called when a player places this block or adds a wire to a wire block
	// also called on the client of the placing player
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (!worldIn.isClientSide())
		{
			for (Direction directionToNeighbor : Direction.values())
			{
				BlockPos neighborPos = pos.relative(directionToNeighbor);
				BlockState neighborState = worldIn.getBlockState(neighborPos);
				if (neighborState.isAir())
				{
					this.addEmptyWireToAir(state, worldIn, neighborPos, directionToNeighbor);
				}
			}
			if (worldIn.getBlockEntity(pos) instanceof WireBlockEntity wire)
			{
				wire.setConnectionsWithServerUpdate(getExpandedShapeIndex(state, worldIn, pos));
			}
		}
		this.updateShapeCache(worldIn, pos);
		ExMachinaGameEvents.scheduleSignalGraphUpdate(worldIn, pos);
		super.setPlacedBy(worldIn, pos, state, placer, stack);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState oldState, ServerLevel level, BlockPos pos, boolean isMoving)
	{
		super.affectNeighborsAfterRemoval(oldState, level, pos, isMoving);
		this.updateShapeCache(level, pos);
	}
	
	// called when a neighboring blockstate changes, not called on the client
	@Override
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block neighborBlock, Orientation orientation, boolean isMoving)
	{
		boolean doGraphUpdate = true;
		// if this is an empty wire block, remove it if no edges are valid anymore
		if (this.isEmptyWireBlock(state))
		{
			worldIn.scheduleTick(pos, this, 1);
			doGraphUpdate = false;
		}
		// if this is a non-empty wire block and the changed state is an air block
		else
		{
			// check neighbors for empty blocks and add empty wires where needed
			for (Direction directionToNeighbor : Direction.values())
			{
				BlockPos fromPos = pos.relative(directionToNeighbor);
				if (worldIn.isEmptyBlock(fromPos))
				{
					this.addEmptyWireToAir(state, worldIn, fromPos, directionToNeighbor);
				}
			}
		}
		if (worldIn.getBlockEntity(pos) instanceof WireBlockEntity wire)
		{
			wire.setConnectionsWithServerUpdate(this.getExpandedShapeIndex(wire.getBlockState(), worldIn, pos));
		}
		this.updateShapeCache(worldIn, pos);
		if (doGraphUpdate)
		{
			ExMachinaGameEvents.scheduleSignalGraphUpdate(worldIn, pos);
		}
		// if the changed neighbor has any convex edges through this block, propagate neighbor update along any edges
//		if (edgeFlags != 0)
//		{
//			EnumSet<Direction> edgeUpdateDirs = EnumSet.noneOf(Direction.class);
//			Edge[] edges = Edge.values();
//			for (int edgeFlag = 0; edgeFlag < 12; edgeFlag++)
//			{
//				if ((edgeFlags & (1 << edgeFlag)) != 0)
//				{
//					Edge edge = edges[edgeFlag];
//					if (edge.sideA == directionToNeighbor)
//						edgeUpdateDirs.add(edge.sideB);
//					else if (edge.sideB == directionToNeighbor)
//						edgeUpdateDirs.add(edge.sideA);
//				}
//			}
//			if (!edgeUpdateDirs.isEmpty() && !EventHooks.onNeighborNotify(worldIn, pos, state, edgeUpdateDirs, false).isCanceled())
//			{
//				BlockPos.MutableBlockPos mutaPos = pos.mutable();
//				for (Direction dir : edgeUpdateDirs)
//				{
//					BlockPos neighborPos = mutaPos.setWithOffset(pos, dir);
//					worldIn.neighborChanged(neighborPos, this, orientation);
//				}
//			}
//		}
		
		super.neighborChanged(state, worldIn, pos, neighborBlock, orientation, isMoving);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		BlockState result = state;
		for (int i=0; i<4; i++) // rotations only rotated about the y-axis, we only need to rotated the horizontal faces
		{
			Direction dir = Direction.from2DDataValue(i);
			Direction newDir = rot.rotate(dir);
			result = result.setValue(INTERIOR_FACES[newDir.ordinal()], state.getValue(INTERIOR_FACES[dir.ordinal()]));
		}
		result = EightGroup.rotate(result, rot);
		return result;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		BlockState result = state;
		for (int i=0; i<4; i++) // only horizontal sides get mirrored
		{
			Direction dir = Direction.from2DDataValue(i);
			Direction newDir = mirrorIn.mirror(dir);
			result = result.setValue(INTERIOR_FACES[newDir.ordinal()], state.getValue(INTERIOR_FACES[dir.ordinal()]));
		}
		result = EightGroup.mirror(result, mirrorIn);
		return result;
	}

	public int getWireCount(BlockState state)
	{
		int count = 0;
		for (int side=0; side<6; side++)
		{
			if (state.getValue(INTERIOR_FACES[side]))
			{
				count++;
			}
		}
		return count;
	}
	
	public boolean isEmptyWireBlock(BlockState state)
	{
		return state == this.defaultBlockState();
	}
	
	protected long getEdgeFlags(BlockGetter world, BlockPos pos)
	{
		long result = 0;
		for (int edge=0; edge<12; edge++)
		{
			if (Edge.values()[edge].shouldEdgeRender(world, pos, this))
			{
				result |= (1L << (edge));
			}
		}
		return result;
	}
	
	/**
	 * Returns a long containing the edge flags shifted for node-line-edge format
	 * The first 30 bits will be 0, the next 12 bits contain edge flags in edge-ordinal order
	 * @param world The world for worldpos context
	 * @param pos The position for worldpos context
	 * @return A set of edge flags where the 30 least-significant bits are 0 and the next 12 bits contain edge flags in edge-ordinal order
	 */
	protected long getEdgeFlagsForNodeLineEdgeFormat(BlockGetter world, BlockPos pos)
	{
		return this.getEdgeFlags(world,pos) << 30;
	}
	
	protected void updateShapeCache(Level world, BlockPos pos)
	{
		VoxelCache.invalidate(world, pos);
		for (int i=0; i<6; i++)
		{
			VoxelCache.invalidate(world, pos.relative(Direction.from3DDataValue(i)));
		}
		if (world instanceof ServerLevel)
		{
			ExMachinaGameEvents.scheduleSignalGraphUpdate(world, pos);
		}
	}

	protected void addEmptyWireToAir(BlockState thisState, Level world, BlockPos neighborAirPos, Direction directionToNeighbor)
	{
		Direction directionFromNeighbor = directionToNeighbor.getOpposite();
		for (Direction dir : Direction.values())
		{
			if (dir != directionToNeighbor && dir != directionFromNeighbor)
			{
				BooleanProperty thisAttachmentFace = INTERIOR_FACES[dir.ordinal()];
				if (thisState.getValue(thisAttachmentFace))
				{
					BlockPos diagonalNeighbor = neighborAirPos.relative(dir);
					BooleanProperty neighborAttachmentFace = INTERIOR_FACES[directionFromNeighbor.ordinal()];
					BlockState diagonalState = world.getBlockState(diagonalNeighbor);
					if (diagonalState.getBlock() == this && diagonalState.getValue(neighborAttachmentFace))
					{
						world.setBlockAndUpdate(neighborAirPos, this.defaultBlockState());
						break; // we don't need to set the wire block more than once
					}
				}
			}
		}
	}
	
	public void destroyClickedSegment(BlockState state, Level world, BlockPos pos, Player player, Direction interiorFace, boolean dropItems)
	{
		FaceSegmentBlock.super.destroyClickedSegment(state, world, pos, player, interiorFace, dropItems);
		this.updateShapeCache(world, pos);
	}
	
	/**
	 * Get the index of the expanded shape for the given wireblock blockstate (including non-blockstate-based shapes);
	 * this is a bit pattern, the bits are defined as such starting from the least-significant bit:
	 * bits 0-5 -- the unexpanded shape index for a given state (indicating which of the six interior faces wires are attached to)
	 * bits 6-29 (the next 24 bits) -- the flags indicating which adjacent neighbors the faces are connecting to
	 * 	each primary face can connect to the faces of four neighbors (6*4 = 24)
	 * bits 30-41 (the next 12 bits) -- the flags indicating any edges connecting wires diagonally
	 * @param state A blockstate belonging to a WireBlock
	 * @param world world
	 * @param pos position of the blockstate
	 * @return An index usable by the voxel cache
	 */
	public long getExpandedShapeIndex(BlockState state, Level world, BlockPos pos)
	{
		// for each of the six interior faces a wire block can have a wire attached to,
		// that face can be connected to any of the four orthagonally adjacent blocks
		// producing an additional component of the resulting shape
		// 6*4 = 24, we have 24 boolean properties, so we have 2^24 different possible shapes
		// we can store these in a bit pattern for efficient keying
		long result = 0;
		for (int side = 0; side < 6; side++)
		{
			// we have to have a wire attached to a side to connect to secondary sides
			BooleanProperty attachmentSide = INTERIOR_FACES[side];
			if (state.getValue(attachmentSide))
			{
				result |= (1L << side);
				Direction attachmentDirection = Direction.from3DDataValue(side);
				for (int subSide = 0; subSide < 4; subSide++)
				{
					int secondaryOrdinal = DirectionHelper.uncompressSecondSide(side, subSide);
					if (state.getValue(INTERIOR_FACES[secondaryOrdinal]))
						continue; // dont add lines if the state already has an elbow here
					Direction secondaryDir = Direction.from3DDataValue(secondaryOrdinal);
					Direction directionToWire = secondaryDir.getOpposite();
					BlockPos neighborPos = pos.relative(secondaryDir);
					BlockState neighborState = world.getBlockState(neighborPos);
					Block neighborBlock = neighborState.getBlock();
					// first, check if the neighbor state is a wire that shares this attachment side
					if (this.canAdjacentBlockConnectToFace(world, pos, state, neighborBlock, attachmentDirection, directionToWire, neighborPos, neighborState))
					{
						result |= (1L << (side*4 + subSide + 6));
					}
				}
			}
		}
		result = result | this.getEdgeFlagsForNodeLineEdgeFormat(world,pos);
		return result;
	}
	
	public VoxelShape getCachedExpandedShapeVoxel(BlockState wireState, Level world, BlockPos pos)
	{
		long index = world.getBlockEntity(pos) instanceof WireBlockEntity wire
			? wire.getConnections()
			: 0;
		
		return this.voxelCache.getUnchecked(index);
	}
	
	public ChannelSet getChannels()
	{
		return this.channels;
	}
	
	public boolean readAttachedPower()
	{
		return this.readAttachedPower;
	}
	
	protected Map<Channel, Collection<TransmissionNode>> createNodes(Level level, BlockPos thisPos, BlockState thisState)
	{
		Map<Channel, Collection<TransmissionNode>> nodesByChannel = new HashMap<>();
		Block thisBlock = thisState.getBlock();
		for (Channel channel : this.channels.channels()) {
			Collection<TransmissionNode> nodesOnChannel = new ArrayList<>();

			// first six connections are physical nodes
			for (int attachmentSideIndex=0; attachmentSideIndex<6; attachmentSideIndex++)
			{
				if (!thisState.getValue(INTERIOR_FACES[attachmentSideIndex]))
					continue;
				
				
				Direction attachmentSide = Direction.values()[attachmentSideIndex];
				Set<Direction> powerReaders = new HashSet<>();
				Set<SignalGraphKey> connectableNodes = new HashSet<>();
				var levelKey = level.dimension();
				if (this.readAttachedPower)
				{
					powerReaders.add(attachmentSide);
				}
				if (this.useIndirectPower)
				{
					// add strong-power connections to wires on the other sides of attached block
					BlockPos neighborPos = thisPos.relative(attachmentSide);
					if (level.getBlockState(neighborPos).isRedstoneConductor(level, neighborPos))
					{
						for (Direction strongNeighborAttachmentSide : Direction.values())
						{
							if (strongNeighborAttachmentSide == attachmentSide)
								continue;
							Direction directionFromCubeToStrongNeighbor = strongNeighborAttachmentSide.getOpposite();
							BlockPos strongNeighborPos = neighborPos.relative(directionFromCubeToStrongNeighbor);
							NodeShape strongNeighborNodeShape = NodeShape.ofSide(strongNeighborAttachmentSide);
							connectableNodes.add(new SignalGraphKey(levelKey, strongNeighborPos, strongNeighborNodeShape, channel));
						}
					}
				}
				// how does this work
				// we have transmission nodes at each node we have a side attached to
				// and the line/edge flags determine what nodes we can connect to (these were already calculated)
				// the next 24 flags are lines
				for (int subsideIndex = 0; subsideIndex < 4; subsideIndex++)
				{
					Direction directionToLineNeighbor = Direction.values()[DirectionHelper.uncompressSecondSide(attachmentSideIndex, subsideIndex)];
					boolean hasElbow = thisState.getValue(INTERIOR_FACES[directionToLineNeighbor.ordinal()]);
					if (hasElbow)
					{
						// if there's another wire node attached in the elbow direction, we can't form a line connection
						// but we can connect to the other half of the elbow
						connectableNodes.add(new SignalGraphKey(levelKey, thisPos, NodeShape.ofSide(directionToLineNeighbor), channel));
					}
					else
					{
						// if block in that direction is another wire block
						// and we can form a convex connection to another wire block THROUGH that block
						// then add an extra connectable face to the convex-connected face
						// otherwise add a parallel connectable face
						BlockPos lineNeighborPos = thisPos.relative(directionToLineNeighbor);
						BlockState neighborState = level.getBlockState(lineNeighborPos);
						Block neighborBlock = neighborState.getBlock();
						BlockPos convexNeighborPos = lineNeighborPos.relative(attachmentSide);
						if (neighborBlock == thisBlock
							&& level.getBlockState(convexNeighborPos).getBlock() == thisBlock)
						{
							connectableNodes.add(new SignalGraphKey(levelKey, convexNeighborPos, NodeShape.ofSide(directionToLineNeighbor.getOpposite()), channel));
						}
						else
						{
							// not a line to a reacharound neighbor, just a regular ol' line
							// add a parallel node
							connectableNodes.add(new SignalGraphKey(levelKey, lineNeighborPos, NodeShape.ofSideSide(attachmentSide, directionToLineNeighbor.getOpposite()), channel));
						}
					}
				}
				
				NodeShape wireNodeShape = NodeShape.ofSide(attachmentSide);
				nodesOnChannel.add(new TransmissionNode(
					wireNodeShape,
					MoreRed.NO_SOURCE,
					powerReaders,
					connectableNodes,
					(world,power) -> this.onReceivePower(world, thisPos, thisState, attachmentSide, power, channel)
				));
			}
			nodesByChannel.put(channel, nodesOnChannel);
		}
		
		return nodesByChannel;
	}
	
	
	protected boolean canAdjacentBlockConnectToFace(Level world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		// check convex edges first
		if (neighborBlock == this)
		{
			BlockPos otherPos = neighborPos.relative(attachmentDirection);
			BlockState otherState = world.getBlockState(otherPos);
			if (otherState.getBlock() == this && otherState.getValue(INTERIOR_FACES[directionToWire.ordinal()]))
			{
				return true;
			}
		}
		StateWirer neighborWirer = StateWirer.getOrDefault(world, neighborPos);
		// check endpoints and transmitters
		var levelKey = world.dimension();
		SignalComponent neighborComponent = neighborWirer.component();
		
		for (Channel ourChannel : this.channels.channels())
		{
			SignalGraphKey wireNode = new SignalGraphKey(levelKey, thisPos, NodeShape.ofSide(attachmentDirection), ourChannel);
			for (Channel theirChannel : ourChannel.getConnectableChannels())
			{
				var neighborNodes = neighborComponent.getTransmissionNodes(levelKey, world, neighborPos, neighborState, theirChannel);
				for (TransmissionNode node : neighborNodes)
				{
					for (SignalGraphKey keyPreferredByNeighbor : node.connectableNodes())
					{
						if (wireNode.isValidFor(keyPreferredByNeighbor))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public void handleLeftClickBlock(LeftClickBlock event, Level level, BlockPos pos, BlockState state)
	{
		if (event.getAction() != LeftClickBlock.Action.START)
			return;
		
		if (!(level instanceof ServerLevel serverLevel))
			return;
		
		Player player = event.getEntity();
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		
		// override event from here
		event.setCanceled(true);
		
		// we still have to redo a few of the existing checks
		if (!serverPlayer.isWithinBlockInteractionRange(pos, 1.0)
			|| (pos.getY() >= serverLevel.getMaxY() || pos.getY() < serverLevel.getMinY())
			|| !serverLevel.mayInteract(serverPlayer, pos) // checks spawn protection and world border
			|| CommonHooks.fireBlockBreak(serverLevel, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos, state).isCanceled()
			|| serverPlayer.blockActionRestricted(serverLevel, pos, serverPlayer.gameMode.getGameModeForPlayer()))
		{
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			return;
		}
		Direction hitNormal = event.getFace();
		Direction destroySide = hitNormal.getOpposite();
		if (serverPlayer.isCreative())
		{
			this.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, false);
			return;
		}
		if (!state.canHarvestBlock(serverLevel, pos, serverPlayer))
		{
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			return;
		}
		this.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, true);
	}

	@Override
	public Map<Direction, VoxelShape> getRaytraceBackboards()
	{
		return this.raytraceBackboards;
	}
}
