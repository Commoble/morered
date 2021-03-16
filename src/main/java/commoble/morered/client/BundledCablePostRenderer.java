package commoble.morered.client;

import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WirePostTileEntity;
import commoble.morered.wire_post.WireSpoolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class BundledCablePostRenderer extends TileEntityRenderer<WirePostTileEntity>
{
	public static final RenderType CABLE_RENDER_TYPE = ExtraRenderTypes.CABLE_RENDER_TYPE;

	public static final RenderMaterial MATERIAL = new RenderMaterial(AtlasTexture.LOCATION_BLOCKS,new ResourceLocation("morered:block/bundled_network_cable")); 

	public static final float[] REDS = {0.71F, 0.31F, 0.19F};
	public static final float[] GREENS = {0.19F, 0.48F, 0.29F};
	public static final float[] BLUES = {0.12F, 0.16F, 0.66F};
	
	public BundledCablePostRenderer(TileEntityRendererDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
	}

	// heavily based on fishing rod line renderer
	@Override
	public void render(WirePostTileEntity post, float partialTicks, MatrixStack matrices, IRenderTypeBuffer buffer, int combinedLightIn, int combinedOverlayIn)
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
		Vector3d postVector = WirePostTileEntity.getConnectionVector(postPos);
		Set<BlockPos> connections = post.getRemoteConnections();
		World world = post.getLevel();
		IVertexBuilder vertexBuilder = buffer.getBuffer(ExtraRenderTypes.CABLE_RENDER_TYPE);
		for (BlockPos connectionPos : connections)
		{
			int startY = postPos.getY();
			int endY = connectionPos.getY();
			// ensure that for each pair of connections, only one cable is rendered (from the lower post if possible)
			if (startY < endY || (startY == endY && postPos.hashCode() < connectionPos.hashCode()))
			{
				this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, postVector, WirePostTileEntity.getConnectionVector(connectionPos), postPos, connectionPos, quadStartU, quadEndU, quadStartV, quadEndV);
			}
		}

		@SuppressWarnings("resource")
		PlayerEntity player = Minecraft.getInstance().player;
		for (Hand hand : Hand.values())
		{
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof WireSpoolItem)
			{
				CompoundNBT nbt = stack.getTagElement(WireSpoolItem.LAST_POST_POS);
				if (nbt != null)
				{
					EntityRendererManager renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
					BlockPos positionOfCurrentPostOfPlayer = NBTUtil.readBlockPos(nbt);

					if (positionOfCurrentPostOfPlayer.equals(postPos))
					{
						Vector3d vectorOfCurrentPostOfPlayer = Vector3d.atCenterOf(positionOfCurrentPostOfPlayer);
						int handSideID = -(hand == Hand.MAIN_HAND ? -1 : 1) * (player.getMainArm() == HandSide.RIGHT ? 1 : -1);

						float swingProgress = player.getAttackAnim(partialTicks);
						float swingZ = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
						float playerAngle = MathHelper.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * ((float) Math.PI / 180F);
						double playerAngleX = MathHelper.sin(playerAngle);
						double playerAngleZ = MathHelper.cos(playerAngle);
						double handOffset = handSideID * 0.35D;
						double handX;
						double handY;
						double handZ;
						float eyeHeight;
						
						// first person
						if ((renderManager.options == null || renderManager.options.getCameraType() == PointOfView.FIRST_PERSON))
						{
							double fov = renderManager.options.fov;
							fov = fov / 100.0D;
							Vector3d handVector = new Vector3d(-0.14 + handSideID * -0.36D * fov, -0.12 + -0.045D * fov, 0.4D);
							handVector = handVector.xRot(-MathHelper.lerp(partialTicks, player.xRotO, player.xRot) * ((float) Math.PI / 180F));
							handVector = handVector.yRot(-MathHelper.lerp(partialTicks, player.yRotO, player.yRot) * ((float) Math.PI / 180F));
							handVector = handVector.yRot(swingZ * 0.5F);
							handVector = handVector.xRot(-swingZ * 0.7F);
							handX = MathHelper.lerp(partialTicks, player.xo, player.getX()) + handVector.x;
							handY = MathHelper.lerp(partialTicks, player.yo, player.getY()) + handVector.y;
							handZ = MathHelper.lerp(partialTicks, player.zo, player.getZ()) + handVector.z;
							eyeHeight = player.getEyeHeight();
						}
						
						// third person
						else
						{
							handX = MathHelper.lerp(partialTicks, player.xo, player.getX()) - playerAngleZ * handOffset - playerAngleX * 0.8D;
							handY = -0.2 + player.yo + player.getEyeHeight() + (player.getY() - player.yo) * partialTicks - 0.45D;
							handZ = MathHelper.lerp(partialTicks, player.zo, player.getZ()) - playerAngleX * handOffset + playerAngleZ * 0.8D;
							eyeHeight = player.isCrouching() ? -0.1875F : 0.0F;
						}
						Vector3d renderPlayerVec = new Vector3d(handX, handY + eyeHeight, handZ);
							
						this.renderConnection(post, world, partialTicks, matrices, vertexBuilder, vectorOfCurrentPostOfPlayer, renderPlayerVec, postPos, player.blockPosition(), quadStartU, quadEndU, quadStartV, quadEndV);
					
					}
				}
			}
		}
	}

	private void renderConnection(WirePostTileEntity post, World world, float partialTicks,
		MatrixStack matrices, IVertexBuilder vertexBuilder, Vector3d startVec, Vector3d endVec, BlockPos startPos, BlockPos endPos,
		float minU, float maxU, float minV, float maxV)
	{
		matrices.pushPose();

		boolean translateSwap = false;
		if (startVec.y() > endVec.y())
		{
			Vector3d swap = startVec;
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
			Vector3d[] pointList = SlackInterpolator.getInterpolatedDifferences(endVec.subtract(startVec));
			int points = pointList.length;
			int lines = points - 1;
			float cableWidth = 0.1F;
			Matrix4f lastMatrix = matrices.last().pose();
			float offsetScale = MathHelper.fastInvSqrt(dx * dx + dz * dz) * cableWidth / 2.0F;
			float xOffset = dz * offsetScale;
			float zOffset = dx * offsetScale;
			int startBlockLight = world.getBrightness(LightType.BLOCK, startPos);
			int endBlockLight = world.getBrightness(LightType.BLOCK, endPos);
			int startSkyLight = world.getBrightness(LightType.SKY, startPos);
			int endSkyLight = world.getBrightness(LightType.SKY, endPos);
			float maxLerpFactor = lines - 1F;

			for (int segmentIndex = 0; segmentIndex < lines; ++segmentIndex)
			{
				Vector3d firstPoint = pointList[segmentIndex];
				Vector3d secondPoint = pointList[segmentIndex+1];
				float lerpFactor = segmentIndex / maxLerpFactor;
				int lerpedBlockLight = (int) MathHelper.lerp(lerpFactor, startBlockLight, endBlockLight);
				int lerpedSkyLight = (int) MathHelper.lerp(lerpFactor, startSkyLight, endSkyLight);
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
					float width = cableWidth * 0.5F;
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
	public boolean shouldRenderOffScreen(WirePostTileEntity te)
	{
		return true;
	}

	public static void addVertexPair(IVertexBuilder vertexBuilder, Matrix4f lastMatrix, int packedLight, float x, float y, float z, float cableWidth,
		float cableWidthOrZero, int segmentIndex, boolean secondVertexPairForQuad, float xOffset, float zOffset,
		float minU, float maxU, float minV, float maxV)
	{
		
		if (!secondVertexPairForQuad)
		{
			vertexBuilder.vertex(lastMatrix, x + xOffset, y + cableWidth - cableWidthOrZero, z - zOffset)
				.color(1F, 1F, 1F, 1F)
				.uv(minU, minV)
				.uv2(packedLight)
				.endVertex();
			vertexBuilder.vertex(lastMatrix, x - xOffset, y + cableWidthOrZero, z + zOffset)
				.color(1F, 1F, 1F, 1F)
				.uv(minU, maxV)
				.uv2(packedLight)
				.endVertex();
		}
		else
		{

			vertexBuilder.vertex(lastMatrix, x - xOffset, y + cableWidthOrZero, z + zOffset)
				.color(1F, 1F, 1F, 1F)
				.uv(maxU, maxV)
				.uv2(packedLight)
				.endVertex();
			vertexBuilder.vertex(lastMatrix, x + xOffset, y + cableWidth - cableWidthOrZero, z - zOffset)
				.color(1F, 1F, 1F, 1F)
				.uv(maxU, minV)
				.uv2(packedLight)
				.endVertex();
		}

	}

}
