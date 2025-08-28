package net.commoble.morered.wires;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoxelCache
{	
	public static VoxelShape get(Level level, BlockPos pos)
	{
		int chunkX = SectionPos.blockToSectionCoord(pos.getX());
		int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
		var chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (chunk == null)
			return Shapes.empty();
		
		var map = chunk.getData(MoreRed.VOXEL_CACHE_ATTACHMENT.get());
		@Nullable VoxelShape cachedShape = map.get(pos);
		if (cachedShape != null)
		{
			return cachedShape;
		}
		
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		
		if (!(block instanceof AbstractWireBlock wireBlock))
		{
			return Shapes.empty();
		}
		VoxelShape wireShape = wireBlock.getCachedExpandedShapeVoxel(state, level, pos);
		map.put(pos.immutable(), wireShape);
		return wireShape;
	}
	
	public static void invalidate(Level level, BlockPos pos)
	{
		int chunkX = SectionPos.blockToSectionCoord(pos.getX());
		int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
		var chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (chunk == null)
			return;
		var map = chunk.getData(MoreRed.VOXEL_CACHE_ATTACHMENT.get());
		map.remove(pos);
	}
}
