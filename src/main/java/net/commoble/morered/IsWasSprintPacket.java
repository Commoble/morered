package net.commoble.morered;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record IsWasSprintPacket(boolean isSprintHeld) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<IsWasSprintPacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.id("is_was_sprint"));
	
	public static final StreamCodec<ByteBuf, IsWasSprintPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, p -> p.isSprintHeld,
		IsWasSprintPacket::new);
	
	public void handle(IPayloadContext context)
	{
		// PlayerData needs to be threadsafed, packet handling is done on worker threads, delegate to main thread
		context.enqueueWork(() -> PlayerData.setSprinting(context.player().getUUID(), this.isSprintHeld));
	}
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
