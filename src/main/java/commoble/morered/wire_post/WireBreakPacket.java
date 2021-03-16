package commoble.morered.wire_post;

import java.util.function.Supplier;

import commoble.morered.client.ClientPacketHandlers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

public class WireBreakPacket
{
	public final Vector3d start;
	public final Vector3d end;
	
	public WireBreakPacket(Vector3d start, Vector3d end)
	{
		this.start = start;
		this.end = end;
	}
	
	public void write(PacketBuffer buffer)
	{
		CompoundNBT nbt = new CompoundNBT();
		nbt.putDouble("startX", this.start.x);
		nbt.putDouble("startY", this.start.y);
		nbt.putDouble("startZ", this.start.z);
		nbt.putDouble("endX", this.end.x);
		nbt.putDouble("endY", this.end.y);
		nbt.putDouble("endZ", this.end.z);
		buffer.writeNbt(nbt);
	}
	
	public static WireBreakPacket read(PacketBuffer buffer)
	{
		CompoundNBT nbt = buffer.readNbt();
		if (nbt == null)
		{
			return new WireBreakPacket(Vector3d.ZERO, Vector3d.ZERO);
		}
		else
		{
			Vector3d start = new Vector3d(
				nbt.getDouble("startX"),
				nbt.getDouble("startY"),
				nbt.getDouble("startZ"));
			Vector3d end = new Vector3d(
				nbt.getDouble("endX"),
				nbt.getDouble("endY"),
				nbt.getDouble("endZ"));
			
			return new WireBreakPacket(start, end);
		}
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		context.enqueueWork(() -> ClientPacketHandlers.onWireBreakPacket(context, this));
		context.setPacketHandled(true);
	}
}
