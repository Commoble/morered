package commoble.morered.client;

import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import commoble.morered.wire_post.AbstractPoweredWirePostBlock;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WirePostTileEntity;
import commoble.morered.wire_post.WireSpoolItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WirePostRenderer implements BlockEntityRenderer<WirePostTileEntity> {

    public WirePostRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static int getRed(Level world, BlockPos pos, BlockState state, float partialTicks) {
        int light = world.getMaxLocalRawBrightness(pos);
        float celestialAngle = world.getSunAngle(partialTicks);
        if (light > 0) {
            float offset = celestialAngle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
            celestialAngle = celestialAngle + (offset - celestialAngle) * 0.2F;
            light = Math.round(light * Mth.cos(celestialAngle));
        }

        light = Math.max(light, world.getBrightness(LightLayer.BLOCK, pos));
        light = Mth.clamp(light, 0, 15);
        int power = state.hasProperty(AbstractPoweredWirePostBlock.POWER) ?
                state.getValue(AbstractPoweredWirePostBlock.POWER) : 0;
        double lerpFactor = power / 15D;
        return (int) Mth.lerp(lerpFactor, ColorHandlers.UNLIT_RED, ColorHandlers.LIT_RED) * light / 15;
    }

    // heavily based on fishing rod line renderer
    @Override
    public void render(WirePostTileEntity post, float partialTicks, PoseStack matrices, MultiBufferSource buffer,
                       int combinedLightIn, int combinedOverlayIn) {
        BlockPos postPos = post.getBlockPos();
        Vec3 postVector = WirePostTileEntity.getConnectionVector(postPos);
        Set<BlockPos> connections = post.getRemoteConnections();
        Level world = post.getLevel();
        BlockState postState = world.getBlockState(postPos);
        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.lines());
        int postRed = getRed(world, postPos, postState, partialTicks);
        for (BlockPos connectionPos : connections) {
            BlockState otherState = world.getBlockState(connectionPos);
            int red = Math.min(postRed, getRed(world, connectionPos, otherState, partialTicks));
            this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, postVector,
                    WirePostTileEntity.getConnectionVector(connectionPos), 0F, red);
        }

        @SuppressWarnings("resource")
        LocalPlayer player = Minecraft.getInstance().player;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof WireSpoolItem) {
                CompoundTag nbt = stack.getTagElement(WireSpoolItem.LAST_POST_POS);
                if (nbt != null) {
                    EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
                    BlockPos positionOfCurrentPostOfPlayer = NbtUtils.readBlockPos(nbt);

                    if (positionOfCurrentPostOfPlayer.equals(postPos)) {
                        Vec3 vectorOfCurrentPostOfPlayer = Vec3.atCenterOf(positionOfCurrentPostOfPlayer);
                        int handSideID =
                                -(hand == InteractionHand.MAIN_HAND ? -1 : 1) * (player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1);

                        float swingProgress = player.getAttackAnim(partialTicks);
                        float swingZ = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                        float playerAngle =
                                Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * ((float) Math.PI / 180F);
                        double playerAngleX = Mth.sin(playerAngle);
                        double playerAngleZ = Mth.cos(playerAngle);
                        double handOffset = handSideID * 0.35D;
                        double d3 = 0.8D;
                        double handX;
                        double handY;
                        double handZ;
                        float eyeHeight;

                        // first person
                        if (renderManager.options.getCameraType() == CameraType.FIRST_PERSON) {
                            double fov = renderManager.options.fov;
                            fov = fov / 100.0D;
                            Vec3 handVector = new Vec3(-0.14 + handSideID * -0.36D * fov, -0.12 + -0.045D * fov, 0.4D);
                            handVector =
                                    handVector.xRot(-Mth.lerp(partialTicks, player.xRotO, player.getXRot()) * ((float) Math.PI / 180F));
                            handVector =
                                    handVector.yRot(-Mth.lerp(partialTicks, player.yRotO, player.getYRot()) * ((float) Math.PI / 180F));
                            handVector = handVector.yRot(swingZ * 0.5F);
                            handVector = handVector.xRot(-swingZ * 0.7F);
                            handX = Mth.lerp(partialTicks, player.xo, player.getX()) + handVector.x;
                            handY = Mth.lerp(partialTicks, player.yo, player.getY()) + handVector.y;
                            handZ = Mth.lerp(partialTicks, player.zo, player.getZ()) + handVector.z;
                            eyeHeight = player.getEyeHeight();
                        }
                        // third person
                        else {
                            handX = Mth.lerp(partialTicks, player.xo, player.getX()) - playerAngleZ * handOffset - playerAngleX * 0.8D;
                            handY = -0.2 + player.yo + player.getEyeHeight() + (player.getY() - player.yo) * partialTicks - 0.45D;
                            handZ = Mth.lerp(partialTicks, player.zo, player.getZ()) - playerAngleX * handOffset + playerAngleZ * 0.8D;
                            eyeHeight = player.isCrouching() ? -0.1875F : 0.0F;
                        }
                        Vec3 renderPlayerVec = new Vec3(handX, handY + eyeHeight, handZ);

                        this.renderConnection(post, world, partialTicks, matrices, vertexBuilder,
                                vectorOfCurrentPostOfPlayer, renderPlayerVec, eyeHeight, postRed);

                    }
                }
            }
        }
    }

    private void renderConnection(WirePostTileEntity post, Level world, float partialTicks,
                                  PoseStack matrices, VertexConsumer vertexBuilder, Vec3 startPos, Vec3 endPos,
                                  float eyeHeight, int red) {
        matrices.pushPose();

        boolean translateSwap = false;
        if (startPos.y() > endPos.y()) {
            Vec3 swap = startPos;
            startPos = endPos;
            endPos = swap;
            translateSwap = true;
        }

        matrices.translate(0.5D, 0.5D, 0.5D);

        double startX = startPos.x();
        double startY = startPos.y();
        double startZ = startPos.z();

        double endX = endPos.x();
        double endY = endPos.y();
        double endZ = endPos.z();
        float dx = (float) (endX - startX);
        float dy = (float) (endY - startY);
        float dz = (float) (endZ - startZ);
        if (translateSwap) {
            matrices.translate(-dx, -dy, -dz);
        }
        Matrix4f fourMatrix = matrices.last().pose();

        if (startY <= endY) {
            Vec3[] pointList = SlackInterpolator.getInterpolatedDifferences(endPos.subtract(startPos));
            int points = pointList.length;
            int lines = points - 1;

            for (int line = 0; line < lines; line++) {
                Vec3 firstPoint = pointList[line];
                Vec3 secondPoint = pointList[line + 1];
                vertexBuilder.vertex(fourMatrix, (float) firstPoint.x(), (float) firstPoint.y(), (float) firstPoint.z()).color(red, 0, 0,
                        255).endVertex();
                vertexBuilder.vertex(fourMatrix, (float) secondPoint.x(), (float) secondPoint.y(), (float) secondPoint.z()).color(red, 0,
                        0, 255).endVertex();
            }
        }

        matrices.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WirePostTileEntity te) {
        return true;
    }
}
