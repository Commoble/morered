package commoble.morered.redwire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import commoble.morered.MoreRed;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;

public class WireUpdateBuffer extends WorldSavedData
{
	public static final String ID = "morered:wireupdatebuffer";
	
	private Map<ChunkPos, Set<BlockPos>> buffer = new HashMap<>();
	
	public WireUpdateBuffer()
	{
		super(ID);
	}

	public static WireUpdateBuffer get(ServerWorld world)
	{
		return world.getSavedData().getOrCreate(WireUpdateBuffer::new, ID);
	}
	
	public void enqueue(BlockPos pos)
	{
		ChunkPos chunkPos = new ChunkPos(pos);
		this.buffer.computeIfAbsent(chunkPos, $-> new HashSet<BlockPos>()).add(pos);
	}
	
	public void sendPackets(ServerWorld world)
	{
		if (this.buffer.size() > 0)
		{
			this.buffer.forEach((chunkPos, positions) ->
			{
				// ignore and discard unloaded chunks
				if (world.chunkExists(chunkPos.x, chunkPos.z))
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
	public void read(CompoundNBT nbt)
	{
		//noop
	}

	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		return compound; //noop
	}
	
}
