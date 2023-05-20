package commoble.morered.wire_post;

import java.util.function.Supplier;

import commoble.morered.client.ClientPacketHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public class WireBreakPacket
{
	public final Vec3 start;
	public final Vec3 end;
	
	public WireBreakPacket(Vec3 start, Vec3 end)
	{
		this.start = start;
		this.end = end;
	}
	
	public void write(FriendlyByteBuf buffer)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("startX", this.start.x);
		nbt.putDouble("startY", this.start.y);
		nbt.putDouble("startZ", this.start.z);
		nbt.putDouble("endX", this.end.x);
		nbt.putDouble("endY", this.end.y);
		nbt.putDouble("endZ", this.end.z);
		buffer.writeNbt(nbt);
	}
	
	public static WireBreakPacket read(FriendlyByteBuf buffer)
	{
		CompoundTag nbt = buffer.readNbt();
		if (nbt == null)
		{
			return new WireBreakPacket(Vec3.ZERO, Vec3.ZERO);
		}
		else
		{
			Vec3 start = new Vec3(
				nbt.getDouble("startX"),
				nbt.getDouble("startY"),
				nbt.getDouble("startZ"));
			Vec3 end = new Vec3(
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
