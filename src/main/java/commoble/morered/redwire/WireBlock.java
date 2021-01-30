package commoble.morered.redwire;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SixWayBlock;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
	
	public static final VoxelShape[] FACE_PLATES_DUNSWE = 
	{
		Block.makeCuboidShape(0,0,0,16,2,16),
		Block.makeCuboidShape(0,14,0,16,16,16),
		Block.makeCuboidShape(0,0,0,16,16,2),
		Block.makeCuboidShape(0,0,14,16,16,16),
		Block.makeCuboidShape(0,0,0,2,16,16),
		Block.makeCuboidShape(14,0,0,16,16,16)
	};
	
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
			for (int side=0; side<6; side++)
			{
				if ((i & (1 << side)) != 0)
				{
					nextShape = VoxelShapes.or(nextShape, NODE_SHAPES_DUNSWE[side]);
				}
			}
			result[i] = nextShape;
		}
		
		return result;
	});
	
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
	
	public boolean isStateEmpty(BlockState state)
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
	
	@Override
	public boolean removedByPlayer(BlockState state, World world, BlockPos pos, @Nullable PlayerEntity player, boolean willHarvest, FluidState fluid)
	{
		// raytrace against the face voxels, get the one the player is actually pointing at
		// this is invoked when the player has been holding left-click sufficiently long enough for the block to break
			// (so it this is an input response, not a performance-critical method)
		// so we can assume that the player is still looking directly at the block's interaction shape
		// do one raytrace against the state's interaction voxel itself,
		// then compare the hit vector to the six face cuboids
		// and see if any of them contain the hit vector
		// we'll need the player to not be null though
		if (player != null)
		{
			Vector3d startVec = player.getEyePosition(1F);
			Vector3d lookOffset = player.getLook(1F); // unit normal vector in look direction
			double rayTraceDistance = 10D; // standard number used elsewhere
			Vector3d endVec = startVec.add(lookOffset.mul(rayTraceDistance, rayTraceDistance, rayTraceDistance));
			ISelectionContext context = ISelectionContext.forEntity(player);
			
			// raytrace against the entire block's interaction shape
			BlockRayTraceResult raytrace = state.getShape(world, pos, context).rayTrace(startVec, endVec, pos);
			if (raytrace != null && raytrace.getHitVec() != null)
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
					// figure out which part of the shape we clicked
					VoxelShape faceShape = FACE_PLATES_DUNSWE[side];
					
					for (AxisAlignedBB aabb : faceShape.toBoundingBoxList())
					{
						if (aabb.contains(relativeHitVec))
						{
							BooleanProperty sideProperty = INTERIOR_FACES[side];
							if (state.get(sideProperty))
							{
								BlockState newState = state.with(sideProperty, false);
								Block newBlock = newState.getBlock();
								BlockState removedState = newBlock.getDefaultState().with(sideProperty, true);
								Block removedBlock = removedState.getBlock();
								removedBlock.onPlayerDestroy(world, pos, removedState);
								if (willHarvest)
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
								world.setBlockState(pos, newState);
								return false;
							}
						}
					}
				}
			}
			
			// if we failed to removed any particular state, return false so the whole block doesn't get broken
			return false;
		}
		
		// if the player doesn't exist, just break the whole block
		return super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
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
}
