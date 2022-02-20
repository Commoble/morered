package commoble.morered.client;

import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WireBreakPacket;
import commoble.morered.wires.VoxelCache;
import commoble.morered.wires.WireUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public class ClientPacketHandlers {

    public static void onWireBreakPacket(NetworkEvent.Context context, WireBreakPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;

        if (world != null) {
            Vec3[] points = SlackInterpolator.getInterpolatedPoints(packet.start, packet.end);
            ParticleEngine manager = mc.particleEngine;
            BlockState state = Blocks.REDSTONE_WIRE.defaultBlockState();

            for (Vec3 point : points) {
                // TODO check if this is correct replacement of effect
                BlockPos pos = new BlockPos(point);
                Particle p = manager.createParticle(ParticleTypes.EFFECT, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
                if (p != null) {
                    p.setPower(0.2F);
                    p.setPos(pos.getX(), pos.getY(), pos.getZ());
                    p.scale(0.6F);
                }
//				manager.add(
//					new DiggingParticle(world, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D, state)
//						.init(pos).setPower(0.2F).scale(0.6F));
            }
        }
    }

    public static void onWireUpdatePacket(WireUpdatePacket packet) {
        @SuppressWarnings("resource")
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            VoxelCache.get(world).shapesByPos.invalidateAll(packet.getPositions());
        }
    }
}
