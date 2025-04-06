package net.commoble.morered.transportation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.morered.MoreRed;
import net.commoble.morered.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.network.PacketDistributor;

public class TubesInChunk
{
	/** This codec serializes a list-like element **/
	public static final Codec<Set<BlockPos>> TUBE_SET_CODEC = BlockPos.CODEC.listOf().xmap(HashSet::new, ArrayList::new);
	/** half-codec that can be used in other places **/
	public static final MapCodec<Set<BlockPos>> FIELD_CODEC = TUBE_SET_CODEC.fieldOf("positions");
	/** This codec serializes a maplike element, its results can be cast to CompoundNBT**/
	public static final Codec<Set<BlockPos>> CODEC = FIELD_CODEC.codec();
	
	public static void updateTubeSet(ServerLevel level, BlockPos pos, BiConsumer<Set<BlockPos>, BlockPos> consumer)
	{
		LevelChunk chunk = level.getChunkAt(pos);
		if (chunk != null)
		{
			var set = chunk.getData(MoreRed.get().tubesInChunkAttachment.get());
			consumer.accept(set, pos);
			chunk.setData(MoreRed.get().tubesInChunkAttachment.get(), set);
			PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), new SyncTubesInChunkPacket(chunk.getPos(), Set.copyOf(set)));
		}
	}
	
	public static Set<ChunkPos> getRelevantChunkPositionsNearPos(BlockPos pos)
	{
		double range = MoreRed.SERVERCONFIG.maxTubeConnectionRange().get();
		ChunkPos chunkPos = new ChunkPos(pos);
		int chunkRange = (int) Math.ceil(range/16D);
		Set<ChunkPos> set = new HashSet<>();
		for (int xOff = -chunkRange; xOff <= chunkRange; xOff++)
		{
			for (int zOff = -chunkRange; zOff <= chunkRange; zOff++)
			{
				set.add(new ChunkPos(chunkPos.x + xOff, chunkPos.z + zOff));
			}
		}
		
		return set;
	}
	
	public static Set<BlockPos> getTubesInChunk(LevelChunk chunk)
	{
		return chunk.getData(MoreRed.get().tubesInChunkAttachment.get());
	}
	
	/**
	 * Gets the tubes in the chunk if the chunk is loaded, if called on the client uses the synced tube data
	 * @param level Level
	 * @param chunkPos ChunkPos
	 * @return Set of the tubes in the chunk, empty set if chunk not loaded
	 */
	public static Set<BlockPos> getTubesInChunkIfLoaded(LevelAccessor level, ChunkPos chunkPos)
	{
		if (level.isClientSide())
		{
			return ClientProxy.getTubesInChunk(chunkPos);
		}
		else if (level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)
		{
			return getTubesInChunk(chunk);
		}
		else
		{
			return Set.of();	
		}
	}
}
