package net.commoble.morered;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;

public interface FaceSegmentBlock
{
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Util.make(Maps.newEnumMap(Direction.class), map -> {
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
    });
    
    public static BooleanProperty getProperty(Direction dir)
    {
    	return PROPERTY_BY_DIRECTION.get(dir);
    }
	
	public static final Map<Direction, Vec3> HITVECS = Util.makeEnumMap(Direction.class, dir -> new Vec3(
		dir == Direction.WEST ? 0D
			: dir == Direction.EAST ? 1D
			: 0.5D,
		dir == Direction.DOWN ? 0D 
			: dir == Direction.UP ? 1D
			: 0.5D,
		dir == Direction.NORTH ? 0D
			: dir == Direction.SOUTH ? 1D
			: 0.5D));

	public abstract Map<Direction,VoxelShape> getRaytraceBackboards();
    public abstract void handleLeftClickBlock(LeftClickBlock event, Level level, BlockPos pos, BlockState state);
	
	public default void destroyClickedSegment(BlockState state, Level world, BlockPos pos, Player player, Direction interiorFace, boolean dropItems)
	{
		BooleanProperty sideProperty = getProperty(interiorFace);
		BlockState newState = state.setValue(sideProperty, false);
		Block newBlock = newState.getBlock();
		BlockState removedState = newBlock.defaultBlockState().setValue(sideProperty, true);
		Block removedBlock = removedState.getBlock();
		if (dropItems)
		{
			// add player stats and spawn drops
			removedBlock.playerDestroy(world, player, pos, removedState, world.getBlockEntity(pos), player.getMainHandItem().copy());
		}
		if (world.isClientSide())
		{
			// level event 2001 plays break sound and makes break particles
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

	@Nullable
	public default Direction getInteriorFaceToBreak(BlockState state, BlockPos pos, Player player, BlockHitResult raytrace, float partialTicks, Map<Direction, VoxelShape> raytraceBackboards)
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
		for (Direction dir : Direction.values())
		{
			if (state.getValue(getProperty(dir)))
			{
				// figure out which part of the shape we clicked
				VoxelShape faceShape = raytraceBackboards.get(dir);
				
				for (AABB aabb : faceShape.toAabbs())
				{
					if (aabb.contains(relativeHitVec))
					{
						return dir;
					}
				}
			}
		}
		
		// if we failed to removed any particular state, return false so the whole block doesn't get broken
		return null;
	}
}
