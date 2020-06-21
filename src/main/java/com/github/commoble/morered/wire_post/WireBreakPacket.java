package com.github.commoble.morered.wire_post;

import java.util.function.Supplier;

import com.github.commoble.morered.client.ClientPacketHandlers;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.network.NetworkEvent;

public class WireBreakPacket
{
	public final Vec3d start;
	public final Vec3d end;
	
	public WireBreakPacket(Vec3d start, Vec3d end)
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
		buffer.writeCompoundTag(nbt);
	}
	
	public static WireBreakPacket read(PacketBuffer buffer)
	{
		CompoundNBT nbt = buffer.readCompoundTag();
		if (nbt == null)
		{
			return new WireBreakPacket(Vec3d.ZERO, Vec3d.ZERO);
		}
		else
		{
			Vec3d start = new Vec3d(
				nbt.getDouble("startX"),
				nbt.getDouble("startY"),
				nbt.getDouble("startZ"));
			Vec3d end = new Vec3d(
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
