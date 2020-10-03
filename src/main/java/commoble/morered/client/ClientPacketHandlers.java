package commoble.morered.client;

import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WireBreakPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClientPacketHandlers
{
	
	public static void onWireBreakPacket(NetworkEvent.Context context, WireBreakPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientWorld world = mc.world;
		
		if (world != null)
		{
			Vec3d[] points = SlackInterpolator.getInterpolatedPoints(packet.start, packet.end);
			ParticleManager manager = mc.particles;
			BlockState state = Blocks.REDSTONE_WIRE.getDefaultState();
			
			for (Vec3d point : points)
			{
				BlockPos pos = new BlockPos(point);
				manager.addEffect(
					new DiggingParticle(world, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D, state)
						.setBlockPos(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
			}
		}
	}
}
