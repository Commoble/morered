package commoble.morered;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketTypeFactory {
    /**
     * @param <PACKET>      the packet class
     * @param id            A packet type id unique to this packet type within the context of the channel we are
     *                      registering to
     * @param channel       The SimpleChannel to register a packet type to
     * @param codec         A codec for the packet's class
     * @param defaultPacket If a packet instance cannot be parsed on the client, it will be treated as this packet
     *                      instance instead.
     *                      The defaultPacket instance must have the same class instance as any packets sent of this
     *                      type,
     *                      and cannot have the same class instance as any other registered packet.
     *                      (lambdas and anonymous classes are fine as long as a different declaration is used for
     *                      each packet type)
     */

    public static <PACKET extends Consumer<NetworkEvent.Context>> void register(int id, SimpleChannel channel,
                                                                                Codec<PACKET> codec,
                                                                                PACKET defaultPacket) {

        final BiConsumer<PACKET, FriendlyByteBuf> encoder = (packet, buffer) -> codec.encodeStart(NbtOps.INSTANCE,
                        packet)
                .result()
                .ifPresent(nbt -> buffer.writeNbt((CompoundTag) nbt));
        final Function<FriendlyByteBuf, PACKET> decoder = buffer -> codec.parse(NbtOps.INSTANCE, buffer.readNbt())
                .result()
                .orElse(defaultPacket);    // we should return a packet without throwing an error here if we don't
        // want logspam
        final BiConsumer<PACKET, Supplier<NetworkEvent.Context>> handler = (packet, context) -> {
            packet.accept(context.get());
            context.get().setPacketHandled(true);
        };

        @SuppressWarnings("unchecked") final Class<PACKET> packetClass = (Class<PACKET>) (defaultPacket.getClass());

        channel.registerMessage(id, packetClass, encoder, decoder, handler);
    }
}
