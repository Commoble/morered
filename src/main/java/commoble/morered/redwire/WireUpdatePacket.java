package commoble.morered.redwire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import commoble.morered.client.ClientPacketHandlers;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class WireUpdatePacket implements Consumer<NetworkEvent.Context>
{
	public static final Codec<WireUpdatePacket> CODEC = BlockPos.CODEC.listOf()
		.<Set<BlockPos>>xmap(HashSet::new, ArrayList::new)
		.xmap(WireUpdatePacket::new, WireUpdatePacket::getPositions)
		.fieldOf("positions").codec();
	
	private final Set<BlockPos> positions; public Set<BlockPos> getPositions() { return this.positions; }
	
	public WireUpdatePacket(Set<BlockPos> positions)
	{
		this.positions = positions;
	}

	@Override
	public void accept(Context context)
	{
		context.enqueueWork(() -> ClientPacketHandlers.onWireUpdatePacket(this));
	}

}
