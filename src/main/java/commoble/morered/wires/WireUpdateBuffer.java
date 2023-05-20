package commoble.morered.wires;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

public class WireUpdateBuffer extends SavedData
{
	public static final String ID = "morered:wireupdatebuffer";
	
	private Map<ChunkPos, Set<BlockPos>> buffer = new HashMap<>();
	
	public static WireUpdateBuffer get(ServerLevel world)
	{
		return world.getDataStorage().computeIfAbsent(tag -> new WireUpdateBuffer(), WireUpdateBuffer::new, ID);
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
					PacketTarget target = PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunk(chunkPos.x, chunkPos.z));
					WireUpdatePacket packet = new WireUpdatePacket(positions);
					MoreRed.CHANNEL.send(target,packet);
				}
			});
			
			this.buffer = new HashMap<>();
		}
	}

	@Override
	public CompoundTag save(CompoundTag compound)
	{
		return compound; //noop
	}
	
}
