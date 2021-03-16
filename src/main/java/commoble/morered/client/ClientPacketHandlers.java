package commoble.morered.client;

import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WireBreakPacket;
import commoble.morered.wires.VoxelCache;
import commoble.morered.wires.WireUpdatePacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClientPacketHandlers
{
	
	public static void onWireBreakPacket(NetworkEvent.Context context, WireBreakPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientWorld world = mc.level;
		
		if (world != null)
		{
			Vector3d[] points = SlackInterpolator.getInterpolatedPoints(packet.start, packet.end);
			ParticleManager manager = mc.particleEngine;
			BlockState state = Blocks.REDSTONE_WIRE.defaultBlockState();
			
			for (Vector3d point : points)
			{
				BlockPos pos = new BlockPos(point);
				manager.add(
					new DiggingParticle(world, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D, state)
						.init(pos).setPower(0.2F).scale(0.6F));
			}
		}
	}
	
	public static void onWireUpdatePacket(WireUpdatePacket packet)
	{
		@SuppressWarnings("resource")
		ClientWorld world = Minecraft.getInstance().level;
		if (world != null)
		{
			VoxelCache.get(world).shapesByPos.invalidateAll(packet.getPositions());
		}
	}
}
