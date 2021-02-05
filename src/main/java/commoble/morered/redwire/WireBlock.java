package commoble.morered.redwire;

import javax.annotation.Nullable;

import commoble.morered.util.DirectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SixWayBlock;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.BlockFlags;

public class WireBlock extends Block
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE =
	{
		Block.makeCuboidShape(7, 0 ,7, 9, 2, 9),
		Block.makeCuboidShape(7, 14, 7, 9, 16, 9),
		Block.makeCuboidShape(7, 7, 0, 9, 9, 2),
		Block.makeCuboidShape(7, 7, 14, 9, 9, 16),
		Block.makeCuboidShape(0, 7, 7, 2, 9, 9),
		Block.makeCuboidShape(14, 7, 7, 16, 9, 9)
	};
	
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = 
	{
		Block.makeCuboidShape(0,0,0,16,2,16),
		Block.makeCuboidShape(0,14,0,16,16,16),
		Block.makeCuboidShape(0,0,0,16,16,2),
		Block.makeCuboidShape(0,0,14,16,16,16),
		Block.makeCuboidShape(0,0,0,2,16,16),
		Block.makeCuboidShape(14,0,0,16,16,16)
	};
	
	public static final VoxelShape[] LINE_SHAPES = Util.make(() ->
	{		
		double min = 0;
		double max = 16;
		double minPlus = 2;
		double maxMinus = 14;
		double innerMin = 7;
		double innerMax = 9;
		
		VoxelShape[] result =
		{
			Block.makeCuboidShape(innerMin, min, min, innerMax, minPlus, innerMin), // down-north
			Block.makeCuboidShape(innerMin, min, innerMax, innerMax, minPlus, max), // down-south
			Block.makeCuboidShape(min, min, innerMin, innerMin, minPlus, innerMax), // down-west
			Block.makeCuboidShape(innerMax, min, innerMin, max, minPlus, innerMax), // down-east
			Block.makeCuboidShape(innerMin, maxMinus, min, innerMax, max, innerMin), // up-north
			Block.makeCuboidShape(innerMin, maxMinus, innerMax, innerMax, max, max), // up-south
			Block.makeCuboidShape(min, maxMinus, innerMin, innerMin, max, innerMax), // up-west
			Block.makeCuboidShape(innerMax, maxMinus, innerMin, max, max, innerMax), // up-east
			Block.makeCuboidShape(innerMin, min, min, innerMax, innerMin, minPlus), // north-down
			Block.makeCuboidShape(innerMin, innerMax, min, innerMax, max, minPlus), // north-up
			Block.makeCuboidShape(min, innerMin, min, innerMin, innerMax, minPlus), //north-west
			Block.makeCuboidShape(innerMax, innerMin, min, max, innerMax, minPlus), // north-east
			Block.makeCuboidShape(innerMin, min, maxMinus, innerMax, innerMin, max), // south-down
			Block.makeCuboidShape(innerMin, innerMax, maxMinus, innerMax, max, max), // south-up
			Block.makeCuboidShape(min, innerMin, maxMinus, innerMin, innerMax, max), // south-west
			Block.makeCuboidShape(innerMax, innerMin, maxMinus, max, innerMax, max), // south-east
			Block.makeCuboidShape(min, min, innerMin, minPlus, innerMin, innerMax), // west-down
			Block.makeCuboidShape(min, innerMax, innerMin, minPlus, max, innerMax), // west-up
			Block.makeCuboidShape(min, innerMin, min, minPlus, innerMax, innerMin), // west-north
			Block.makeCuboidShape(min, innerMin, innerMax, minPlus, innerMax, max), // west-south
			Block.makeCuboidShape(maxMinus, min, innerMin, max, innerMin, innerMax), // east-down
			Block.makeCuboidShape(maxMinus, innerMax, innerMin, max, max, innerMax), // east-up
			Block.makeCuboidShape(maxMinus, innerMin, min, max, innerMax, innerMin), // east-north
			Block.makeCuboidShape(maxMinus, innerMin, innerMax, max, innerMax, max) // east-south
		};
		
		return result;
	});
	
	public static VoxelShape getLineShape(int side, int secondarySide)
	{
		return LINE_SHAPES[side*4 + secondarySide];
	}
	
	public static final BooleanProperty DOWN = SixWayBlock.DOWN;
	public static final BooleanProperty UP = SixWayBlock.UP;
	public static final BooleanProperty NORTH = SixWayBlock.NORTH;
	public static final BooleanProperty SOUTH = SixWayBlock.SOUTH;
	public static final BooleanProperty WEST = SixWayBlock.WEST;
	public static final BooleanProperty EAST = SixWayBlock.EAST;
	public static final BooleanProperty[] INTERIOR_FACES = {DOWN,UP,NORTH,SOUTH,WEST,EAST};
	
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = Util.make(() ->
	{
		VoxelShape[] result = new VoxelShape[64];
		
		for (int i=0; i<64; i++)
		{
			VoxelShape nextShape = VoxelShapes.empty();
			boolean[] addedSides = new boolean[6]; // sides for which we've already added node shapes to
			for (int side=0; side<6; side++)
			{
				if ((i & (1 << side)) != 0)
				{
					nextShape = VoxelShapes.or(nextShape, NODE_SHAPES_DUNSWE[side]);
					
					int sideAxis = side/2; // 0,1,2 = y,z,x
					for (int secondarySide = 0; secondarySide < side; secondarySide++)
					{
						if (addedSides[secondarySide] && sideAxis != secondarySide/2) // the two sides are orthagonal to each other (not parallel)
						{
							// add line shapes for the elbows
							nextShape = VoxelShapes.or(nextShape, getLineShape(side, DirectionHelper.getCompressedSecondSide(side,secondarySide)));
							nextShape = VoxelShapes.or(nextShape, getLineShape(secondarySide, DirectionHelper.getCompressedSecondSide(secondarySide,side)));
						}
					}
					
					addedSides[side] = true;
				}
			}
			result[i] = nextShape;
		}
		
		return result;
	});
	
	/**
	 * Get the index of the primary shape for the given wireblock blockstate (64 combinations)
	 * @param state A blockstate belonging to WireBlock
	 * @return An index useable in SHAPES_BY_STATE_INDEX
	 */
	public static int getShapeIndex(BlockState state)
	{
		int index = 0;
		int sideCount = INTERIOR_FACES.length;
		for (int side=0; side < sideCount; side++)
		{
			if (state.get(INTERIOR_FACES[side]))
			{
				index |= 1 << side;
			}
		}
		
		return index;
	}

	public WireBlock(Properties properties)
	{
		super(properties);
		// the "default" state has to be the empty state so we can build it up one face at a time
		this.setDefaultState(this.getStateContainer().getBaseState()
			.with(DOWN, false)
			.with(UP, false)
			.with(NORTH, false)
			.with(SOUTH, false)
			.with(WEST, false)
			.with(EAST, false)
			);
	}
	
	public boolean isEmptyWireBlock(BlockState state)
	{
		return state == this.getDefaultState();
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder)
	{
		super.fillStateContainer(builder);
		builder.add(DOWN,UP,NORTH,SOUTH,WEST,EAST);
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		return worldIn instanceof World
			? VoxelCache.get((World)worldIn).getWireShape(pos)
			: SHAPES_BY_STATE_INDEX[getShapeIndex(state)];
	}

	// overriding this so we don't delegate to getShape for the render shape (to reduce lookups of the extended shape)
	@Override
	public VoxelShape getRenderShape(BlockState state, IBlockReader worldIn, BlockPos pos)
	{
		return SHAPES_BY_STATE_INDEX[getShapeIndex(state)];
	}

	@Override
	public BlockState updatePostPlacement(BlockState thisState, Direction directionToNeighbor, BlockState neighborState, IWorld world, BlockPos thisPos, BlockPos neighborPos)
	{
		BooleanProperty sideProperty = INTERIOR_FACES[directionToNeighbor.ordinal()];
		if (thisState.get(sideProperty)) // if wire is attached on the relevant face, check if it should be disattached
		{
			Direction neighborSide = directionToNeighbor.getOpposite();
			boolean isNeighborSideSolid = neighborState.isSolidSide(world, neighborPos, neighborSide);
			if (!isNeighborSideSolid && world instanceof ServerWorld)
			{
				Block.spawnDrops(this.getDefaultState().with(sideProperty, true), (ServerWorld)world, thisPos);
			}
			return thisState.with(sideProperty, isNeighborSideSolid);
		}
		else
		{
			return thisState;
		}
	}

	@Override
	public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos)
	{
		Direction[] dirs = Direction.values();
		for (int side=0; side<6; side++)
		{
			if (state.get(INTERIOR_FACES[side]))
			{
				Direction dir = dirs[side];
				BlockPos neighborPos = pos.offset(dir);
				BlockState neighborState = world.getBlockState(neighborPos);
				Direction neighborSide = dir.getOpposite();
				if (!neighborState.isSolidSide(world, neighborPos, neighborSide))
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
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		World world = context.getWorld();
		Direction sideOfNeighbor = context.getFace();
		Direction directionToNeighbor = sideOfNeighbor.getOpposite();
		// with standard BlockItemUseContext rules,
		// if replacing a block, this is the position of the block we're replacing
		// otherwise, it's the position adjacent to the clicked block (offset by sideOfNeighbor)
		BlockPos placePos = context.getPos();
		BlockPos neighborPos = placePos.offset(directionToNeighbor);
		BlockState neighborState = world.getBlockState(neighborPos);
		return neighborState.isSolidSide(world, neighborPos, sideOfNeighbor)
			? this.getDefaultState().with(INTERIOR_FACES[directionToNeighbor.ordinal()], true)
			: null;
	}
	/**
	 * Spawn a digging particle effect in the world, this is a wrapper around
	 * EffectRenderer.addBlockHitEffects to allow the block more control over the
	 * particles. Useful when you have entirely different texture sheets for
	 * different sides/locations in the world.
	 *
	 * @param state
	 *            The current state
	 * @param world
	 *            The current world
	 * @param target
	 *            The target the player is looking at {x/y/z/side/sub}
	 * @param manager
	 *            A reference to the current particle manager.
	 * @return True to prevent vanilla digging particles form spawning.
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean addHitEffects(BlockState state, World worldObj, RayTraceResult target, ParticleManager manager)
	{
		return this.getWireCount(state) == 0; // if we have no wires here, we have no shape, so return true to disable particles
		// (otherwise the particle manager crashes because it performs an unsupported operation on the empty shape)
	}

	@Override
	public boolean isReplaceable(BlockState state, BlockItemUseContext useContext)
	{
		return this.getWireCount(state) == 0;
	}
	
	// called when a different block instance is replaced with this one
	// only called on server
	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		this.updateShapeCache(worldIn, pos);
		super.onBlockAdded(state, worldIn, pos, oldState, isMoving);
	}

	// called when a player places this block or adds a wire to a wire block
	// also called on the client of the placing player
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (!worldIn.isRemote)
		{
			for (Direction directionToNeighbor : Direction.values())
			{
				BlockPos neighborPos = pos.offset(directionToNeighbor);
				BlockState neighborState = worldIn.getBlockState(neighborPos);
				if (neighborState.isAir())
				{
					this.addEmptyWireToAir(state, worldIn, neighborPos, directionToNeighbor);
				}
			}
		}
		this.updateShapeCache(worldIn, pos);
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
	}

	// called when the block is destroyed or a player removes a wire from a wire block
	// also called on the client of the breaking player
	@Override
	public void onReplaced(BlockState oldState, World worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		// if this is an empty wire block, remove it if no edges are valid anymore
		if (this.isEmptyWireBlock(newState))
		{
			long edgeFlags = getEdgeFlags(worldIn,pos);
			if (edgeFlags == 0)
			{
				worldIn.removeBlock(pos, false);
			}
		}
		this.updateShapeCache(worldIn, pos);
		super.onReplaced(oldState, worldIn, pos, newState, isMoving);
	}

	// called when a neighboring blockstate changes, not called on the client
	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		// if this is an empty wire block, remove it if no edges are valid anymore
		if (this.isEmptyWireBlock(state))
		{
			long edgeFlags = getEdgeFlags(worldIn,pos);
			if (edgeFlags == 0)
			{
				worldIn.removeBlock(pos, false);
			}
		}
		// if this is a non-empty wire block and the changed state is an air block
		else if (worldIn.isAirBlock(fromPos))
		{
			BlockPos offset = fromPos.subtract(pos);
			Direction directionToNeighbor = Direction.byLong(offset.getX(), offset.getY(), offset.getZ());
			if (directionToNeighbor != null)
			{
				this.addEmptyWireToAir(state, worldIn, fromPos, directionToNeighbor);
			}
		}

		this.updateShapeCache(worldIn, pos);
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
	}
	
	
	
	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		BlockState result = state;
		for (int i=0; i<4; i++) // rotations only rotated about the y-axis, we only need to rotated the horizontal faces
		{
			Direction dir = Direction.byHorizontalIndex(i);
			Direction newDir = rot.rotate(dir);
			result = result.with(INTERIOR_FACES[newDir.ordinal()], state.get(INTERIOR_FACES[dir.ordinal()]));
		}
		return result;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		BlockState result = state;
		for (int i=0; i<4; i++) // only horizontal sides get mirrored
		{
			Direction dir = Direction.byHorizontalIndex(i);
			Direction newDir = mirrorIn.mirror(dir);
			result = result.with(INTERIOR_FACES[newDir.ordinal()], state.get(INTERIOR_FACES[dir.ordinal()]));
		}
		return result;
	}

	protected void addEmptyWireToAir(BlockState thisState, World world, BlockPos neighborAirPos, Direction directionToNeighbor)
	{
		Direction directionFromNeighbor = directionToNeighbor.getOpposite();
		for (Direction dir : Direction.values())
		{
			if (dir != directionToNeighbor && dir != directionFromNeighbor)
			{
				BooleanProperty thisAttachmentFace = INTERIOR_FACES[dir.ordinal()];
				if (thisState.get(thisAttachmentFace))
				{
					BlockPos diagonalNeighbor = neighborAirPos.offset(dir);
					BooleanProperty neighborAttachmentFace = INTERIOR_FACES[directionFromNeighbor.ordinal()];
					BlockState diagonalState = world.getBlockState(diagonalNeighbor);
					if (diagonalState.getBlock() == this.getBlock() && diagonalState.get(neighborAttachmentFace))
					{
						world.setBlockState(neighborAirPos, this.getDefaultState());
						break; // we don't need to set the wire block more than once
					}
				}
			}
		}
	}
	
	public void updateShapeCache(World world, BlockPos pos)
	{
		VoxelCache.get(world).shapesByPos.invalidate(pos);
		if (world instanceof ServerWorld)
		{
			WireUpdateBuffer.get((ServerWorld)world).enqueue(pos);
		}
	}

	public int getWireCount(BlockState state)
	{
		int count = 0;
		for (int side=0; side<6; side++)
		{
			if (state.get(INTERIOR_FACES[side]))
			{
				count++;
			}
		}
		return count;
	}
	
	@Nullable
	public Direction getInteriorFaceToBreak(BlockState state, BlockPos pos, PlayerEntity player, BlockRayTraceResult raytrace, float partialTicks)
	{
		// raytrace against the face voxels, get the one the player is actually pointing at
		// this is invoked when the player has been holding left-click sufficiently long enough for the block to break
			// (so it this is an input response, not a performance-critical method)
		// so we can assume that the player is still looking directly at the block's interaction shape
		// do one raytrace against the state's interaction voxel itself,
		// then compare the hit vector to the six face cuboids
		// and see if any of them contain the hit vector
		// we'll need the player to not be null though
//		if (player != null)
		{
//			Vector3d startVec = player.getEyePosition(partialTicks);
			Vector3d lookOffset = player.getLook(partialTicks); // unit normal vector in look direction
//			double rayTraceDistance = 10D; // standard number used elsewhere
//			Vector3d endVec = startVec.add(lookOffset.mul(rayTraceDistance, rayTraceDistance, rayTraceDistance));
//			ISelectionContext context = ISelectionContext.forEntity(player);
			
			// raytrace against the entire block's interaction shape
//			BlockRayTraceResult raytrace = state.getShape(world, pos, context).rayTrace(startVec, endVec, pos);
//			if (raytrace != null && raytrace.getHitVec() != null)
			{
				// we want to adjust the hitvec by a very small amount toward the hit voxel
				// because the contains math includes the minimum positions of the box's dimensions but not the maximum
				// and the hit vector's position is equal to the edge position of the voxel
				Vector3d hitVec = raytrace.getHitVec();
				Vector3d relativeHitVec = hitVec
					.add(lookOffset.mul(0.001D, 0.001D, 0.001D))
					.subtract(pos.getX(), pos.getY(), pos.getZ()); // we're also wanting this to be relative to the voxel
				for (int side=0; side<6; side++)
				{
					if (state.get(INTERIOR_FACES[side]))
					{
						// figure out which part of the shape we clicked
						VoxelShape faceShape = RAYTRACE_BACKBOARDS[side];
						
						
						for (AxisAlignedBB aabb : faceShape.toBoundingBoxList())
						{
							if (aabb.contains(relativeHitVec))
							{
								return Direction.byIndex(side);
//								BooleanProperty sideProperty = INTERIOR_FACES[side];
//								if (state.get(sideProperty))
//								{
//									BlockState newState = state.with(sideProperty, false);
//									Block newBlock = newState.getBlock();
//									BlockState removedState = newBlock.getDefaultState().with(sideProperty, true);
//									Block removedBlock = removedState.getBlock();
//									removedBlock.onPlayerDestroy(world, pos, removedState);
//									if (willHarvest)
//									{
//										// add player stats and spawn drops
//										removedBlock.harvestBlock(world, player, pos, removedState, null, player.getHeldItemMainhand().copy());
//									}
//									if (world.isRemote)
//									{
//										// on the server world, onBlockHarvested will play the break effects for each player except the player given
//										// and also anger piglins at that player, so we do want to give it the player
//										// on the client world, willHarvest is always false, so we need to manually play the effects for that player
//										world.playEvent(player, 2001, pos, Block.getStateId(removedState));
//									}
//									else
//									{
//										// plays the break event for every nearby player except the breaking player
//										removedBlock.onBlockHarvested(world, pos, removedState, player);
//									}
//									world.setBlockState(pos, newState);
//								}
							}
						}
					}
				}
			}
			
			// if we failed to removed any particular state, return false so the whole block doesn't get broken
			return null;
		}
	}
	
	public void destroyClickedSegment(BlockState state, World world, BlockPos pos, PlayerEntity player, Direction interiorFace, boolean dropItems)
	{
		int side = interiorFace.ordinal();
		BooleanProperty sideProperty = INTERIOR_FACES[side];
		if (state.get(sideProperty))
		{
			BlockState newState = state.with(sideProperty, false);
			Block newBlock = newState.getBlock();
			BlockState removedState = newBlock.getDefaultState().with(sideProperty, true);
			Block removedBlock = removedState.getBlock();
			if (dropItems)
			{
				// add player stats and spawn drops
				removedBlock.harvestBlock(world, player, pos, removedState, null, player.getHeldItemMainhand().copy());
			}
			if (world.isRemote)
			{
				// on the server world, onBlockHarvested will play the break effects for each player except the player given
				// and also anger piglins at that player, so we do want to give it the player
				// on the client world, willHarvest is always false, so we need to manually play the effects for that player
				world.playEvent(player, 2001, pos, Block.getStateId(removedState));
			}
			else
			{
				// plays the break event for every nearby player except the breaking player
				removedBlock.onBlockHarvested(world, pos, removedState, player);
			}
			// default and rerender flags are used when block is broken on client
			if (world.setBlockState(pos, newState, BlockFlags.DEFAULT_AND_RERENDER))
			{
				removedBlock.onPlayerDestroy(world, pos, removedState);
			}
		}
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
	public long getExpandedShapeIndex(BlockState state, IBlockReader world, BlockPos pos)
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
			if (state.get(attachmentSide))
			{
				result |= (1L << side);
				for (int subSide = 0; subSide < 4; subSide++)
				{
					int secondaryOrdinal = DirectionHelper.uncompressSecondSide(side, subSide);
					Direction secondaryDir = Direction.byIndex(secondaryOrdinal);
					Block thisBlock = state.getBlock();
					BlockPos neighborPos = pos.offset(secondaryDir);
					BlockState neighborState = world.getBlockState(neighborPos);
					Block neighborBlock = neighborState.getBlock();
					boolean isNeighborWire = neighborBlock == thisBlock;
					// first, check if the neighbor state is a wire that shares this attachment side
					// TODO filter out wire block connectability for different types of wires
					if (isNeighborWire && neighborState.get(attachmentSide))
					{
						result |= (1L << (side*4 + subSide + 6));
					}
					// otherwise, check if we can connect through a wire edge
					else if (isNeighborWire || neighborState.isAir())
					{
						Direction primaryDir = Direction.byIndex(side);
						BlockPos diagonalPos = neighborPos.offset(primaryDir);
						BlockState diagonalState = world.getBlockState(diagonalPos);
						if (diagonalState.getBlock() == thisBlock)
						{
							Direction diagonalAttachmentDir = secondaryDir.getOpposite();
							if (diagonalState.get(INTERIOR_FACES[diagonalAttachmentDir.ordinal()]))
							{
								result |= (1L << (side*4 + subSide + 6));
							}
						}
					}
					// otherwise, check if we can redstone-connect to the neighbor block
					else if (neighborState.canConnectRedstone(world, neighborPos, secondaryDir))
					{
						VoxelShape lineShape = getLineShape(side, subSide);
						VoxelShape neighborShape = neighborState.getRenderShape(world, neighborPos); // block support shape
						VoxelShape projectedNeighborShape = neighborShape.project(secondaryDir.getOpposite());
						// if the projected neighbor shape entirely overlaps the line shape,
						// then the neighbor shape can be connected to by the wire
						// we can test this by doing an ONLY_SECOND comparison on the shapes
						// if this returns true, then there are places where the second shape is not overlapped by the first
						// so if this returns false, then we can proceed
						if (!VoxelShapes.compare(projectedNeighborShape, lineShape, IBooleanFunction.ONLY_SECOND))
						{
							result |= (1L << (side*4 + subSide + 6));
						}
					}
				}
			}
		}
		result = result | getEdgeFlags(world,pos);
		return result;
	}
	
	public static long getEdgeFlags(IBlockReader world, BlockPos pos)
	{
		long result = 0;
		for (int edge=0; edge<12; edge++)
		{
			if (Edge.values()[edge].shouldEdgeRender(world, pos))
			{
				result |= (1L << (30 + edge));
			}
		}
		return result;
	}
	
	public static VoxelShape makeExpandedShapeForIndex(long index)
	{
		int primaryShapeIndex = (int) (index & 63);
		long expandedShapeIndex = index >> 6;
		VoxelShape shape = SHAPES_BY_STATE_INDEX[primaryShapeIndex];
		// we want to use the index to combine secondary line shapes with the actual voxelshape for a given state
		int flag = 1;
		for (int side = 0; side < 6; side++)
		{
			for (int subSide = 0; subSide < 4; subSide++)
			{
				if ((expandedShapeIndex & flag) != 0)
				{
					shape = VoxelShapes.or(shape, getLineShape(side, subSide));
				}
				flag = flag << 1;
			}
		}
		return shape;
	}
}
