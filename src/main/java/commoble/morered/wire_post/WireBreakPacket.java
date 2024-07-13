package commoble.morered.wire_post;

import commoble.morered.MoreRed;
import commoble.morered.client.ClientProxy;
import commoble.morered.util.MoreCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WireBreakPacket(Vec3 start, Vec3 end) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<WireBreakPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.getModRL("wire_break"));
	public static final StreamCodec<ByteBuf, WireBreakPacket> STREAM_CODEC = StreamCodec.composite(
		MoreCodecs.VEC3_STREAM_CODEC, WireBreakPacket::start,
		MoreCodecs.VEC3_STREAM_CODEC, WireBreakPacket::end,
		WireBreakPacket::new);
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
	
	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> ClientProxy.onWireBreakPacket(this));
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
