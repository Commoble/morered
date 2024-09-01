package net.commoble.morered.wires;

import java.util.List;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import net.commoble.morered.MoreRed;
import net.commoble.morered.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WireUpdatePacket(Set<BlockPos> positions) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<WireUpdatePacket> TYPE = new CustomPacketPayload.Type<>(MoreRed.getModRL("wire_update"));
	public static final StreamCodec<ByteBuf, WireUpdatePacket> STREAM_CODEC = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())
		.map(Set::copyOf, List::copyOf)
		.map(WireUpdatePacket::new, WireUpdatePacket::positions);
	
	public void handle(IPayloadContext context)
	{
		context.enqueueWork(() -> ClientProxy.onWireUpdatePacket(this));
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

}
