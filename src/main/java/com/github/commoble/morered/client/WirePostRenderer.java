package com.github.commoble.morered.client;

import java.util.List;
import java.util.Set;

import com.github.commoble.morered.wire_post.SlackInterpolator;
import com.github.commoble.morered.wire_post.WirePostBlock;
import com.github.commoble.morered.wire_post.WirePostTileEntity;
import com.github.commoble.morered.wire_post.WireSpoolItem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class WirePostRenderer extends TileEntityRenderer<WirePostTileEntity>
{

	public WirePostRenderer(TileEntityRendererDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
	}
	
	public static int getRed(World world, BlockPos pos, BlockState state, float partialTicks)
	{
		int light = world.getLight(pos);
		float celestialAngle = world.getCelestialAngleRadians(partialTicks);
		if (light > 0)
		{
			float offset = celestialAngle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
			celestialAngle = celestialAngle + (offset - celestialAngle) * 0.2F;
			light = Math.round(light * MathHelper.cos(celestialAngle));
		}

		light = Math.max(light, world.getLightFor(LightType.BLOCK, pos));
		light = MathHelper.clamp(light, 0, 15);
		int power = state.has(WirePostBlock.POWER) ? state.get(WirePostBlock.POWER) : 0;
		double lerpFactor = power / 15D;
		return (int)MathHelper.lerp(lerpFactor, ColorHandlers.UNLIT_RED, ColorHandlers.LIT_RED) * light / 15;
	}

	// heavily based on fishing rod line renderer
	@Override
	public void render(WirePostTileEntity post, float partialTicks, MatrixStack matrices, IRenderTypeBuffer buffer, int combinedLightIn, int combinedOverlayIn)
	{
		BlockPos postPos = post.getPos();
		Vec3d postVector = new Vec3d(postPos).add(0.5D, 0.5D, 0.5D);
		Set<BlockPos> connections = post.getRemoteConnections();
		World world = post.getWorld();
		BlockState postState = world.getBlockState(postPos);
		IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.getLines());
		int postRed = getRed(world, postPos, postState, partialTicks);
		for (BlockPos connectionPos : connections)
		{
			BlockState otherState = world.getBlockState(connectionPos);
			int red = Math.min(postRed, getRed(world, connectionPos, otherState, partialTicks));
			this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, postVector, new Vec3d(connectionPos).add(0.5D, 0.5D, 0.5D), 0F, red);
		}

		@SuppressWarnings("resource")
		PlayerEntity player = Minecraft.getInstance().player;
		for (Hand hand : Hand.values())
		{
			ItemStack stack = player.getHeldItem(hand);
			if (stack.getItem() instanceof WireSpoolItem)
			{
				CompoundNBT nbt = stack.getChildTag(WireSpoolItem.LAST_POST_POS);
				if (nbt != null)
				{
					EntityRendererManager renderManager = Minecraft.getInstance().getRenderManager();
					BlockPos positionOfCurrentPostOfPlayer = NBTUtil.readBlockPos(nbt);

					if (positionOfCurrentPostOfPlayer.equals(postPos))
					{
						Vec3d vectorOfCurrentPostOfPlayer = new Vec3d(positionOfCurrentPostOfPlayer).add(0.5d, 0.5d, 0.5d);
						int handSideID = -(hand == Hand.MAIN_HAND ? -1 : 1) * (player.getPrimaryHand() == HandSide.RIGHT ? 1 : -1);

						float swingProgress = player.getSwingProgress(partialTicks);
						float swingZ = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
						float playerAngle = MathHelper.lerp(partialTicks, player.prevRenderYawOffset, player.renderYawOffset) * ((float) Math.PI / 180F);
						double playerAngleX = MathHelper.sin(playerAngle);
						double playerAngleZ = MathHelper.cos(playerAngle);
						double handOffset = handSideID * 0.35D;
						double d3 = 0.8D;
						double handX;
						double handY;
						double handZ;
						float eyeHeight;
						if ((renderManager.options == null || renderManager.options.thirdPersonView <= 0))
						{
							double fov = renderManager.options.fov;
							fov = fov / 100.0D;
							Vec3d handVector = new Vec3d(handSideID * -0.36D * fov, -0.045D * fov, 0.4D);
							handVector = handVector.rotatePitch(-MathHelper.lerp(partialTicks, player.prevRotationPitch, player.rotationPitch) * ((float) Math.PI / 180F));
							handVector = handVector.rotateYaw(-MathHelper.lerp(partialTicks, player.prevRotationYaw, player.rotationYaw) * ((float) Math.PI / 180F));
							handVector = handVector.rotateYaw(swingZ * 0.5F);
							handVector = handVector.rotatePitch(-swingZ * 0.7F);
							handX = MathHelper.lerp(partialTicks, player.prevPosX, player.getPosX()) + handVector.x;
							handY = MathHelper.lerp(partialTicks, player.prevPosY, player.getPosY()) + handVector.y;
							handZ = MathHelper.lerp(partialTicks, player.prevPosZ, player.getPosZ()) + handVector.z;
							eyeHeight = player.getEyeHeight();
						}
						else
						{
							handX = MathHelper.lerp(partialTicks, player.prevPosX, player.getPosX()) - playerAngleZ * handOffset - playerAngleX * 0.8D;
							handY = player.prevPosY + player.getEyeHeight() + (player.getPosY() - player.prevPosY) * partialTicks - 0.45D;
							handZ = MathHelper.lerp(partialTicks, player.prevPosZ, player.getPosZ()) - playerAngleX * handOffset + playerAngleZ * 0.8D;
							eyeHeight = player.isCrouching() ? -0.1875F : 0.0F;
						}
						Vec3d renderPlayerVec = new Vec3d(handX, handY + eyeHeight, handZ);
							
						this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, vectorOfCurrentPostOfPlayer, renderPlayerVec, eyeHeight, postRed);
					
					}
				}
			}
		}
	}

	private void renderConnection(WirePostTileEntity post, World world, float partialTicks,
		MatrixStack matrices, IVertexBuilder vertexBuilder, Vec3d startPos, Vec3d endPos, float eyeHeight, int red)
	{
		matrices.push();

		boolean translateSwap = false;
		if (startPos.getY() > endPos.getY())
		{
			Vec3d swap = startPos;
			startPos = endPos;
			endPos = swap;
			translateSwap = true;
		}

		matrices.translate(0.5D, 0.5D, 0.5D);

		double startX = startPos.getX();
		double startY = startPos.getY();
		double startZ = startPos.getZ();

		double endX = endPos.getX();
		double endY = endPos.getY();
		double endZ = endPos.getZ();
		float dx = (float) (endX - startX);
		float dy = (float) (endY - startY);
		float dz = (float) (endZ - startZ);
		if (translateSwap)
		{
			matrices.translate(-dx, -dy, -dz);
		}
		Matrix4f fourMatrix = matrices.getLast().getMatrix();

		if (startY <= endY)
		{
			List<Vec3d> pointList = SlackInterpolator.getInterpolatedDifferences(endPos.subtract(startPos));
			int points = pointList.size();
			int lines = points - 1;
			
			for (int line = 0; line < lines; line++)
			{
				Vec3d firstPoint = pointList.get(line);
				Vec3d secondPoint = pointList.get(line+1);
//				drawNextLineSegment(dx, dy, dz, vertexBuilder, fourMatrix, startLerp, startYLerp, red);
				vertexBuilder.pos(fourMatrix, (float)firstPoint.getX(), (float)firstPoint.getY(), (float)firstPoint.getZ()).color(red, 0, 0, 255).endVertex();
//				drawNextLineSegment(dx, dy, dz, vertexBuilder, fourMatrix, endLerp, endYLerp, red);
				vertexBuilder.pos(fourMatrix, (float)secondPoint.getX(), (float)secondPoint.getY(), (float)secondPoint.getZ()).color(red, 0, 0, 255).endVertex();
			}
		}

		matrices.pop();
	}

	public static void drawNextLineSegment(float x, float y, float z, IVertexBuilder vertexBuilder, Matrix4f fourMatrix, float lerp, float yLerp, int red)
	{
		vertexBuilder.pos(fourMatrix, x * lerp, y * (yLerp), z * lerp).color(red, 0, 0, 255).endVertex();
	}

	public static void spawnParticleFromTo(World world, Vec3d start, Vec3d end)
	{
		double lerp = world.rand.nextDouble();
		double x = MathHelper.lerp(lerp, start.getX(), end.getX());
		double y = MathHelper.lerp(lerp, start.getY(), end.getY());
		double z = MathHelper.lerp(lerp, start.getZ(), end.getZ());

		world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
	}

	@Override
	public boolean isGlobalRenderer(WirePostTileEntity te)
	{
		return true;
	}
}
