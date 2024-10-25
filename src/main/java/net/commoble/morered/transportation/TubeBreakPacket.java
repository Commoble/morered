package net.commoble.morered.transportation;

import io.netty.buffer.ByteBuf;
import net.commoble.morered.MoreRed;
import net.commoble.morered.client.ClientProxy;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TubeBreakPacket(Vec3 start, Vec3 end) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<TubeBreakPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.id("tube_break"));
	public static final StreamCodec<ByteBuf, TubeBreakPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.DOUBLE, p -> p.start.x,
		ByteBufCodecs.DOUBLE, p -> p.start.y,
		ByteBufCodecs.DOUBLE, p -> p.start.z,
		ByteBufCodecs.DOUBLE, p -> p.end.x,
		ByteBufCodecs.DOUBLE, p -> p.end.y,
		ByteBufCodecs.DOUBLE, p -> p.end.z,
		TubeBreakPacket::new);
	
	public TubeBreakPacket(double x0, double y0, double z0, double x1, double y1, double z1)
	{
		this(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1));
	}
	
	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> ClientProxy.onTubeBreakPacket(this));
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
