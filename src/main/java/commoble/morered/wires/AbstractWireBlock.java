package commoble.morered.wires;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import commoble.morered.client.ClientProxy;
import commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

public abstract class AbstractWireBlock extends Block
{
	
	public static final BooleanProperty DOWN = PipeBlock.DOWN;
	public static final BooleanProperty UP = PipeBlock.UP;
	public static final BooleanProperty NORTH = PipeBlock.NORTH;
	public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
	public static final BooleanProperty WEST = PipeBlock.WEST;
	public static final BooleanProperty EAST = PipeBlock.EAST;
	public static final BooleanProperty[] INTERIOR_FACES = {DOWN,UP,NORTH,SOUTH,WEST,EAST};
	
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

	public static boolean canWireConnectToAdjacentWireOrCable(BlockGetter world, BlockPos thisPos,
		BlockState thisState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		// this wire can connect to an adjacent wire's subwire if
		// A) the direction to the other wire is orthagonal to the attachment face of the other wire
		// (e.g. if the other wire is attached to DOWN, then we can connect if it's to the north, south, west, or east
		// and B) this wire is also attached to the same face
		if (wireFace.getAxis() != directionToWire.getAxis() && thisState.getValue(INTERIOR_FACES[wireFace.ordinal()]))
			return true;
		
		// otherwise, check if we can connect through a wire edge
		Block wireBlock = wireState.getBlock();
		if (wireBlock == thisState.getBlock())
		{
			BlockPos diagonalPos = thisPos.relative(wireFace);
			BlockState diagonalState = world.getBlockState(diagonalPos);
			if (diagonalState.getBlock() == wireBlock)
			{
				if (diagonalState.getValue(INTERIOR_FACES[directionToWire.ordinal()]))
				{
					return true;
				}
			}
		}
		return false;
	}

	protected final VoxelShape[] shapesByStateIndex;
	protected final VoxelShape[] raytraceBackboards;
	protected final LoadingCache<Long, VoxelShape> voxelCache;
	protected final boolean useIndirectPower;

	/**
	 * 
	 * @param properties block properties
	 * @param shapesByStateIndex Array of 64 voxelshapes, the voxels for the canonical blockstates
	 * @param raytraceBackboards Array of 6 voxelshapes (by attachment face direction) used for subwire raytrace checking
	 * @param voxelCache The cache to use for this block's voxels given world context
	 * @param useIndirectPower Whether this block is allowed to send or receive power conducted indirectly through solid cubes
	 */
	public AbstractWireBlock(Properties properties, VoxelShape[] shapesByStateIndex, VoxelShape[] raytraceBackboards, LoadingCache<Long, VoxelShape> voxelCache, boolean useIndirectPower)
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
			);
		this.shapesByStateIndex = shapesByStateIndex;
		this.raytraceBackboards = raytraceBackboards;
		this.voxelCache = voxelCache;
		this.useIndirectPower = useIndirectPower;
	}

	
	protected abstract boolean canAdjacentBlockConnectToFace(BlockGetter world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState);
	protected abstract void updatePowerAfterBlockUpdate(Level world, BlockPos wirePos, BlockState wireState);
	protected abstract void notifyNeighbors(Level world, BlockPos wirePos, BlockState newState, EnumSet<Direction> updateDirections, boolean doConductedPowerUpdates);

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DOWN,UP,NORTH,SOUTH,WEST,EAST);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
	{
		return worldIn instanceof Level
			? VoxelCache.get((Level)worldIn).getWireShape(pos)
			: this.shapesByStateIndex[getShapeIndex(state)];
	}

	// overriding this so we don't delegate to getShape for the render shape (to reduce lookups of the extended shape)
	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos)
	{
		return this.shapesByStateIndex[getShapeIndex(state)];
	}

	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
	{
		return this.isEmptyWireBlock(state);
	}

	@Override
	public BlockState updateShape(BlockState thisState, Direction directionToNeighbor, BlockState neighborState, LevelAccessor world, BlockPos thisPos, BlockPos neighborPos)
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

	@Override
	public void initializeClient(Consumer<IClientBlockExtensions> consumer)
	{
		ClientProxy.initializeAbstractWireBlockClient(this, consumer);
	}
	
	// called when a different block instance is replaced with this one
	// only called on server
	// called after previous block and TE are removed, but before this block's TE is added
	@Override
	@Deprecated
	public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		this.updateShapeCache(worldIn, pos);
		super.onPlace(state, worldIn, pos, oldState, isMoving);
	}

	// called when a player places this block or adds a wire to a wire block
	// also called on the client of the placing player
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (!worldIn.isClientSide)
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
		}
		this.updateShapeCache(worldIn, pos);
		this.updatePowerAfterBlockUpdate(worldIn, pos, state);
		super.setPlacedBy(worldIn, pos, state, placer, stack);
	}

	// called when the blockstate at the given pos changes
	// only called on servers
	// oldState is a state of this block, newState may or may not be
	@Override
	@Deprecated
	public void onRemove(BlockState oldState, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		// if this is an empty wire block, remove it if no edges are valid anymore
		boolean doPowerUpdate = true;
		if (newState.getBlock() != this) // wire block was completely destroyed
		{
			this.notifyNeighbors(worldIn, pos, newState, EnumSet.allOf(Direction.class), this.useIndirectPower);
			doPowerUpdate = false;
		}
		else if (this.isEmptyWireBlock(newState)) // wire block has no attached faces and consists only of fake wire edges
		{
			long edgeFlags = this.getEdgeFlags(worldIn,pos);
			if (edgeFlags == 0)	// if we don't need to be rendering any edges, set wire block to air
			{
				// removing the block will call onReplaced again, but using the newBlock != this condition
				worldIn.removeBlock(pos, false);
			}
			else
			{
				// so only notify neighbors if we *don't* completely remove the block
				this.notifyNeighbors(worldIn, pos, newState, EnumSet.allOf(Direction.class), this.useIndirectPower);
			}
			doPowerUpdate = false;
		}
		this.updateShapeCache(worldIn, pos);
		super.onRemove(oldState, worldIn, pos, newState, isMoving);
		// if the new state is still a wire block and has at least one wire in it, do a power update
		if (doPowerUpdate)
			this.updatePowerAfterBlockUpdate(worldIn, pos, newState);
	}

	// called when a neighboring blockstate changes, not called on the client
	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving)
	{
		BlockPos offset = fromPos.subtract(pos);
		Direction directionToNeighbor = Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ());
		long edgeFlags = this.getEdgeFlags(worldIn,pos);
		// if this is an empty wire block, remove it if no edges are valid anymore
		if (this.isEmptyWireBlock(state))
		{
			if (edgeFlags == 0)
			{
				worldIn.removeBlock(pos, false);
			}
		}
		// if this is a non-empty wire block and the changed state is an air block
		else if (worldIn.isEmptyBlock(fromPos))
		{
			if (directionToNeighbor != null)
			{
				this.addEmptyWireToAir(state, worldIn, fromPos, directionToNeighbor);
			}
		}

		this.updateShapeCache(worldIn, pos);
		this.updatePowerAfterBlockUpdate(worldIn, pos, state);
		// if the changed neighbor has any convex edges through this block, propagate neighbor update along any edges
		if (edgeFlags != 0)
		{
			EnumSet<Direction> edgeUpdateDirs = EnumSet.noneOf(Direction.class);
			Edge[] edges = Edge.values();
			for (int edgeFlag = 0; edgeFlag < 12; edgeFlag++)
			{
				if ((edgeFlags & (1 << edgeFlag)) != 0)
				{
					Edge edge = edges[edgeFlag];
					if (edge.sideA == directionToNeighbor)
						edgeUpdateDirs.add(edge.sideB);
					else if (edge.sideB == directionToNeighbor)
						edgeUpdateDirs.add(edge.sideA);
				}
			}
			if (!edgeUpdateDirs.isEmpty() && !net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(worldIn, pos, state, edgeUpdateDirs, false).isCanceled())
			{
				BlockPos.MutableBlockPos mutaPos = pos.mutable();
				for (Direction dir : edgeUpdateDirs)
				{
					BlockPos neighborPos = mutaPos.setWithOffset(pos, dir);
					worldIn.neighborChanged(neighborPos, this, pos);
				}
			}
		}
		
		super.neighborChanged(state, worldIn, pos, neighborBlock, fromPos, isMoving);
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
	
	@Nullable
	public Direction getInteriorFaceToBreak(BlockState state, BlockPos pos, Player player, BlockHitResult raytrace, float partialTicks)
	{
		// raytrace against the face voxels, get the one the player is actually pointing at
		// this is invoked when the player has been holding left-click sufficiently long enough for the block to break
			// (so it this is an input response, not a performance-critical method)
		// so we can assume that the player is still looking directly at the block's interaction shape
		// do one raytrace against the state's interaction voxel itself,
		// then compare the hit vector to the six face cuboids
		// and see if any of them contain the hit vector
		// we'll need the player to not be null though
		Vec3 lookOffset = player.getViewVector(partialTicks); // unit normal vector in look direction
		Vec3 hitVec = raytrace.getLocation();
		Vec3 relativeHitVec = hitVec
			.add(lookOffset.multiply(0.001D, 0.001D, 0.001D))
			.subtract(pos.getX(), pos.getY(), pos.getZ()); // we're also wanting this to be relative to the voxel
		for (int side=0; side<6; side++)
		{
			if (state.getValue(INTERIOR_FACES[side]))
			{
				// figure out which part of the shape we clicked
				VoxelShape faceShape = this.raytraceBackboards[side];
				
				for (AABB aabb : faceShape.toAabbs())
				{
					if (aabb.contains(relativeHitVec))
					{
						return Direction.from3DDataValue(side);
					}
				}
			}
		}
		
		// if we failed to removed any particular state, return false so the whole block doesn't get broken
		return null;
	}
	
	protected void updateShapeCache(Level world, BlockPos pos)
	{
		LoadingCache<BlockPos, VoxelShape> cache = VoxelCache.get(world).shapesByPos;
		cache.invalidate(pos);
		for (int i=0; i<6; i++)
		{
			cache.invalidate(pos.relative(Direction.from3DDataValue(i)));
		}
		if (world instanceof ServerLevel)
		{
			WireUpdateBuffer.get((ServerLevel)world).enqueue(pos);
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
		int side = interiorFace.ordinal();
		BooleanProperty sideProperty = INTERIOR_FACES[side];
		if (state.getValue(sideProperty))
		{
			BlockState newState = state.setValue(sideProperty, false);
			Block newBlock = newState.getBlock();
			BlockState removedState = newBlock.defaultBlockState().setValue(sideProperty, true);
			Block removedBlock = removedState.getBlock();
			if (dropItems)
			{
				// add player stats and spawn drops
				removedBlock.playerDestroy(world, player, pos, removedState, null, player.getMainHandItem().copy());
			}
			if (world.isClientSide)
			{
				// on the server world, onBlockHarvested will play the break effects for each player except the player given
				// and also anger piglins at that player, so we do want to give it the player
				// on the client world, willHarvest is always false, so we need to manually play the effects for that player
				world.levelEvent(player, 2001, pos, Block.getId(removedState));
			}
			else
			{
				// plays the break event for every nearby player except the breaking player
				removedBlock.playerWillDestroy(world, pos, removedState, player);
			}
			// default and rerender flags are used when block is broken on client
			if (world.setBlock(pos, newState, Block.UPDATE_ALL_IMMEDIATE))
			{
				removedBlock.destroy(world, pos, removedState);
			}
		}
		
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
	public long getExpandedShapeIndex(BlockState state, BlockGetter world, BlockPos pos)
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
		long index = this.getExpandedShapeIndex(wireState, world, pos);
		
		return this.voxelCache.getUnchecked(index);
	}
}
