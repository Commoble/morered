package commoble.morered.client;

import java.util.Set;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import commoble.morered.MoreRed;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WirePostBlockEntity;
import commoble.morered.wire_post.WireSpoolItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BundledCablePostRenderer implements BlockEntityRenderer<WirePostBlockEntity>
{
	public static final RenderType CABLE_RENDER_TYPE = ExtraRenderTypes.CABLE_RENDER_TYPE;

	@SuppressWarnings("deprecation")
	public static final Material MATERIAL = new Material(TextureAtlas.LOCATION_BLOCKS,MoreRed.getModRL("block/bundled_network_cable")); 

	public static final float[] REDS = {0.71F, 0.31F, 0.19F};
	public static final float[] GREENS = {0.19F, 0.48F, 0.29F};
	public static final float[] BLUES = {0.12F, 0.16F, 0.66F};
	
	public BundledCablePostRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	// heavily based on fishing rod line renderer
	@Override
	public void render(WirePostBlockEntity post, float partialTicks, PoseStack matrices, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn)
	{
		TextureAtlasSprite sprite = MATERIAL.sprite();
		float totalMinU = sprite.getU0();
		float totalMinV = sprite.getV0();
		float totalMaxU = sprite.getU1();
		float totalMaxV = sprite.getV1();
		float texWidth = totalMaxU - totalMinU;
		float texHeight = totalMaxV - totalMinV;
		float quadStartU = (11F/16F) * texWidth + totalMinU;
		float quadEndU = (13F/16F) * texWidth + totalMinU;
		float quadStartV = (6F/16F) * texHeight + totalMinV;
		float quadEndV = (12F/16F) * texHeight + totalMinV;
		BlockPos postPos = post.getBlockPos();
		Vec3 postVector = Vec3.atCenterOf(postPos);
		Set<BlockPos> connections = post.getRemoteConnections();
		Level world = post.getLevel();
		VertexConsumer vertexBuilder = buffer.getBuffer(ExtraRenderTypes.CABLE_RENDER_TYPE);
		for (BlockPos connectionPos : connections)
		{
			int startY = postPos.getY();
			int endY = connectionPos.getY();
			// ensure that for each pair of connections, only one cable is rendered (from the lower post if possible)
			if (startY < endY || (startY == endY && postPos.hashCode() < connectionPos.hashCode()))
			{
				this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, postVector, Vec3.atCenterOf(connectionPos), postPos, connectionPos, quadStartU, quadEndU, quadStartV, quadEndV);
			}
		}

		@SuppressWarnings("resource")
		Player player = Minecraft.getInstance().player;
		for (InteractionHand hand : InteractionHand.values())
		{
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof WireSpoolItem)
			{
				BlockPos positionOfCurrentPostOfPlayer = stack.get(MoreRed.get().spooledPostComponent.get());
				if (positionOfCurrentPostOfPlayer != null)
				{
					EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

					if (positionOfCurrentPostOfPlayer.equals(postPos))
					{
						Vec3 vectorOfCurrentPostOfPlayer = Vec3.atCenterOf(positionOfCurrentPostOfPlayer);
						int handSideID = -(hand == InteractionHand.MAIN_HAND ? -1 : 1) * (player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1);

						float swingProgress = player.getAttackAnim(partialTicks);
						float swingZ = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
						float playerAngle = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * ((float) Math.PI / 180F);
						double playerAngleX = Mth.sin(playerAngle);
						double playerAngleZ = Mth.cos(playerAngle);
						double handOffset = handSideID * 0.35D;
						double handX;
						double handY;
						double handZ;
						float eyeHeight;
						
						// first person
						if ((renderManager.options == null || renderManager.options.getCameraType() == CameraType.FIRST_PERSON))
						{
							double fov = renderManager.options.fov().get();
							fov = fov / 100.0D;
							Vec3 handVector = new Vec3(-0.14 + handSideID * -0.36D * fov, -0.12 + -0.045D * fov, 0.4D);
							handVector = handVector.xRot(-Mth.lerp(partialTicks, player.xRotO, player.getXRot()) * ((float) Math.PI / 180F));
							handVector = handVector.yRot(-Mth.lerp(partialTicks, player.yRotO, player.getYRot()) * ((float) Math.PI / 180F));
							handVector = handVector.yRot(swingZ * 0.5F);
							handVector = handVector.xRot(-swingZ * 0.7F);
							handX = Mth.lerp(partialTicks, player.xo, player.getX()) + handVector.x;
							handY = Mth.lerp(partialTicks, player.yo, player.getY()) + handVector.y;
							handZ = Mth.lerp(partialTicks, player.zo, player.getZ()) + handVector.z;
							eyeHeight = player.getEyeHeight();
						}
						
						// third person
						else
						{
							handX = Mth.lerp(partialTicks, player.xo, player.getX()) - playerAngleZ * handOffset - playerAngleX * 0.8D;
							handY = -0.2 + player.yo + player.getEyeHeight() + (player.getY() - player.yo) * partialTicks - 0.45D;
							handZ = Mth.lerp(partialTicks, player.zo, player.getZ()) - playerAngleX * handOffset + playerAngleZ * 0.8D;
							eyeHeight = player.isCrouching() ? -0.1875F : 0.0F;
						}
						Vec3 renderPlayerVec = new Vec3(handX, handY + eyeHeight, handZ);
							
						this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, vectorOfCurrentPostOfPlayer, renderPlayerVec, postPos, player.blockPosition(), quadStartU, quadEndU, quadStartV, quadEndV);
					
					}
				}
			}
		}
	}

	private void renderConnection(WirePostBlockEntity post, Level level, float partialTicks,
		PoseStack matrices, VertexConsumer vertexBuilder, Vec3 startVec, Vec3 endVec, BlockPos startPos, BlockPos endPos,
		float minU, float maxU, float minV, float maxV)
	{
		matrices.pushPose();

		boolean translateSwap = false;
		if (startVec.y() > endVec.y())
		{
			Vec3 swap = startVec;
			startVec = endVec;
			endVec = swap;
			float swapF = minU;
			minU = maxU;
			maxU = swapF;
			swapF = minV;
			minV = maxV;
			maxV = swapF;
			translateSwap = true;
		}

		matrices.translate(0.5D, 0.5D, 0.5D);

		double startX = startVec.x();
		double startY = startVec.y();
		double startZ = startVec.z();

		double endX = endVec.x();
		double endY = endVec.y();
		double endZ = endVec.z();
		float dx = (float) (endX - startX);
		float dy = (float) (endY - startY);
		float dz = (float) (endZ - startZ);
		if (translateSwap)
		{
			matrices.translate(-dx, -dy, -dz);
		}

		if (startY <= endY)
		{
			Vec3[] pointList = SlackInterpolator.getInterpolatedDifferences(endVec.subtract(startVec));
			int points = pointList.length;
			int lines = points - 1;
			float cableWidth = 0.1F;
			Matrix4f lastMatrix = matrices.last().pose();
			@SuppressWarnings("deprecation")
			float offsetScale = 0.5F * cableWidth * (float)Mth.fastInvSqrt(dx * dx + dz * dz);
			float xOffset = dz * offsetScale;
			float zOffset = dx * offsetScale;
			int startBlockLight = level.getBrightness(LightLayer.BLOCK, startPos);
			int endBlockLight = level.getBrightness(LightLayer.BLOCK, endPos);
			int startSkyLight = level.getBrightness(LightLayer.SKY, startPos);
			int endSkyLight = level.getBrightness(LightLayer.SKY, endPos);
			float maxLerpFactor = lines - 1F;

			for (int segmentIndex = 0; segmentIndex < lines; ++segmentIndex)
			{
				Vec3 firstPoint = pointList[segmentIndex];
				Vec3 secondPoint = pointList[segmentIndex+1];
				float lerpFactor = segmentIndex / maxLerpFactor;
				int lerpedBlockLight = (int) Mth.lerp(lerpFactor, startBlockLight, endBlockLight);
				int lerpedSkyLight = (int) Mth.lerp(lerpFactor, startSkyLight, endSkyLight);
				int packedLight = LightTexture.pack(lerpedBlockLight, lerpedSkyLight);
				float x0 = (float) firstPoint.x;
				float y0 = (float) firstPoint.y;
				float z0 = (float) firstPoint.z;
				float x1 = (float) secondPoint.x;
				float y1 = (float) secondPoint.y;
				float z1 = (float) secondPoint.z;
				int secondSegmentIndex = segmentIndex+1;
				
				if (xOffset == 0F && zOffset == 0F) // completely vertical
				{
					// render the quads with a different orientation if the cable is completely vertical
					// (otherwise they have 0 width and are invisible)
					float width = (float) (cableWidth * 0.5F);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x0, y0, z0, cableWidth, cableWidth, segmentIndex, false, width, width, minU, maxU, minV, maxV);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x1, y1, z1, cableWidth, cableWidth, secondSegmentIndex, true, width, width, minU, maxU, minV, maxV);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x0, y0, z0, cableWidth, 0F, segmentIndex, false, -width, width, minU, maxU, minV, maxV);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x1, y1, z1, cableWidth, 0F, secondSegmentIndex, true, -width, width, minU, maxU, minV, maxV);
				}
				else
				{
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x0, y0, z0, cableWidth, cableWidth, segmentIndex, false, xOffset, zOffset, minU, maxU, minV, maxV);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x1, y1, z1, cableWidth, cableWidth, secondSegmentIndex, true, xOffset, zOffset, minU, maxU, minV, maxV);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x0, y0, z0, cableWidth, 0F, segmentIndex, false, xOffset, zOffset, minU, maxU, minV, maxV);
					addVertexPair(vertexBuilder, lastMatrix, packedLight, x1, y1, z1, cableWidth, 0F, secondSegmentIndex, true, xOffset, zOffset, minU, maxU, minV, maxV);
				}
			}
		}

		matrices.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(WirePostBlockEntity te)
	{
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(WirePostBlockEntity blockEntity)
	{
		return blockEntity.getRenderBoundingBox();
	}

	public static void addVertexPair(VertexConsumer vertexBuilder, Matrix4f lastMatrix, int packedLight, float x, float y, float z, float cableWidth,
		float cableWidthOrZero, int segmentIndex, boolean secondVertexPairForQuad, float xOffset, float zOffset,
		float minU, float maxU, float minV, float maxV)
	{
		
		if (!secondVertexPairForQuad)
		{
			vertexBuilder.addVertex(lastMatrix, x + xOffset, y + cableWidth - cableWidthOrZero, z - zOffset)
				.setColor(1F, 1F, 1F, 1F)
				.setUv(minU, minV)
				.setLight(packedLight);
			vertexBuilder.addVertex(lastMatrix, x - xOffset, y + cableWidthOrZero, z + zOffset)
				.setColor(1F, 1F, 1F, 1F)
				.setUv(minU, maxV)
				.setLight(packedLight);
		}
		else
		{

			vertexBuilder.addVertex(lastMatrix, x - xOffset, y + cableWidthOrZero, z + zOffset)
				.setColor(1F, 1F, 1F, 1F)
				.setUv(maxU, maxV)
				.setLight(packedLight);
			vertexBuilder.addVertex(lastMatrix, x + xOffset, y + cableWidth - cableWidthOrZero, z - zOffset)
				.setColor(1F, 1F, 1F, 1F)
				.setUv(maxU, minV)
				.setLight(packedLight);
		}

	}

}
