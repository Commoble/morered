package commoble.morered.wires;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

public class WireUpdateBuffer extends SavedData
{
	public static final String ID = "morered:wireupdatebuffer";
	
	public static final SavedData.Factory<WireUpdateBuffer> FACTORY = new SavedData.Factory<>(WireUpdateBuffer::new, (tag,registries) -> new WireUpdateBuffer());
	
	private Map<ChunkPos, Set<BlockPos>> buffer = new HashMap<>();
	
	public static WireUpdateBuffer get(ServerLevel world)
	{
		return world.getDataStorage().computeIfAbsent(FACTORY, ID);
	}
	
	public void enqueue(BlockPos pos)
	{
		ChunkPos chunkPos = new ChunkPos(pos);
		this.buffer.computeIfAbsent(chunkPos, $-> new HashSet<BlockPos>()).add(pos.immutable());
	}
	
	public void sendPackets(ServerLevel world)
	{
		if (this.buffer.size() > 0)
		{
			this.buffer.forEach((chunkPos, positions) ->
			{
				// ignore and discard unloaded chunks
				if (world.hasChunk(chunkPos.x, chunkPos.z))
				{
					PacketDistributor.sendToPlayersTrackingChunk(world, chunkPos, new WireUpdatePacket(positions));
				}
			});
			
			this.buffer = new HashMap<>();
		}
	}

	@Override
	public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries)
	{
		return compound; //noop
	}
	
}
