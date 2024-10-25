package net.commoble.morered.client;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.transportation.ItemInTubeWrapper;
import net.commoble.morered.transportation.PliersItem;
import net.commoble.morered.transportation.RaytraceHelper;
import net.commoble.morered.transportation.RemoteConnection;
import net.commoble.morered.transportation.TubeBlock;
import net.commoble.morered.transportation.TubeBlockEntity;
import net.commoble.morered.util.BlockSide;
import net.commoble.morered.util.DirectionTransformer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TubeBlockEntityRenderer implements BlockEntityRenderer<TubeBlockEntity>
{

	public TubeBlockEntityRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	@Override
	public void render(TubeBlockEntity tube, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int combinedLight, int combinedOverlay)
	{
		// render tick happens independently of regular ticks and often more frequently
		if (!tube.inventory.isEmpty())
		{
			for (ItemInTubeWrapper wrapper : tube.inventory)
			{
				this.renderWrapper(tube, wrapper, partialTicks, matrix, buffer, combinedLight);
			}
		}
		if (!tube.incomingWrapperBuffer.isEmpty())
		{
			for (ItemInTubeWrapper wrapper : tube.incomingWrapperBuffer)
			{
				this.renderWrapper(tube, wrapper, partialTicks, matrix, buffer, combinedLight);
			}
		}
		this.renderLongTubes(tube, partialTicks, matrix, buffer, combinedLight, combinedOverlay);
	}

	// ** copied from entity ItemRenderer **//

	protected int getModelCount(ItemStack stack)
	{
		int i = 1;
		if (stack.getCount() > 48)
		{
			i = 5;
		}
		else if (stack.getCount() > 32)
		{
			i = 4;
		}
		else if (stack.getCount() > 16)
		{
			i = 3;
		}
		else if (stack.getCount() > 1)
		{
			i = 2;
		}

		return i;
	}

	/**
	 * Renders an itemstack
	 */
	public void renderWrapper(TubeBlockEntity tube, ItemInTubeWrapper wrapper, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int intA)
	{
		Direction nextMove = wrapper.remainingMoves.peek();
		if (nextMove == null)
			return;
		ItemStack itemstack = wrapper.stack;
		ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer(); // itemrenderer knows how to render items
		
		@SuppressWarnings("resource")
		RandomSource random = tube.getLevel().random;
		
		Item item = itemstack.getItem();
		int renderSeed = itemstack.isEmpty() ? 187 : Item.getId(item) + itemstack.getDamageValue(); // the random is used to offset sub-items
		random.setSeed(renderSeed);
		

		matrix.pushPose();
		int renderedItemCount = this.getModelCount(itemstack);
		float xStart, yStart, zStart, xEnd, yEnd, zEnd;
		float lerpFactor = (wrapper.ticksElapsed + partialTicks) / wrapper.maximumDurationInTube;	// factor in range [0,1)
		Vec3 renderOffset;
		float remoteScale = 1F; // extra scaling if rendering in a narrow remote tube
		if (wrapper.freshlyInserted)	// first move
		{
			xEnd = 0F;
			yEnd = 0F;
			zEnd = 0F;
			xStart = xEnd - nextMove.getStepX();
			yStart = yEnd - nextMove.getStepY();
			zStart = zEnd - nextMove.getStepZ();
			float xLerp = Mth.lerp(lerpFactor, xStart, xEnd);
			float yLerp = Mth.lerp(lerpFactor, yStart, yEnd);
			float zLerp = Mth.lerp(lerpFactor, zStart, zEnd);
			renderOffset = new Vec3(xLerp, yLerp, zLerp);
		}
		else	// any other move
		{
			renderOffset = getItemRenderOffset(tube, nextMove, lerpFactor);
			remoteScale = (float)getItemRenderScale(tube, nextMove, lerpFactor);
		}

//		itemRenderer.blitOffset -= 50F;
		for (int currentModelIndex = 0; currentModelIndex < renderedItemCount; ++currentModelIndex)
		{
			matrix.pushPose();
			float xAdjustment = 0F;
			float yAdjustment = 0F;
			float zAdjustment = 0F;
			if (currentModelIndex > 0)
			{
				xAdjustment = (random.nextFloat() * 2.0F - 1.0F) * 0.01F;
				yAdjustment = (random.nextFloat() * 2.0F - 1.0F) * 0.01F;
				zAdjustment = (random.nextFloat() * 2.0F - 1.0F) * 0.01F;
			}
			float xTranslate = (float) (renderOffset.x + xAdjustment + 0.5F);
			float yTranslate = (float) (renderOffset.y + yAdjustment + 0.4375F);
			float zTranslate = (float) (renderOffset.z + zAdjustment + 0.5F);
			matrix.translate(xTranslate, yTranslate, zTranslate);// aggregate is centered
			float scale = remoteScale * 0.5F;
			matrix.scale(scale, scale, scale);
			
			itemRenderer.renderStatic(itemstack, ItemDisplayContext.GROUND, intA, OverlayTexture.NO_OVERLAY, matrix, buffer, tube.getLevel(), renderSeed);
			matrix.popPose();
		}
//		itemRenderer.blitOffset += 50F;

		matrix.popPose();
	}
	
	/**
	 * Get the render offset to render a travelling item
	 * @param tube
	 * @param travelDirection
	 * @param lerpFactor
	 * @return
	 */
	public static Vec3 getItemRenderOffset(TubeBlockEntity tube, Direction travelDirection, float lerpFactor)
	{
		@Nullable RemoteConnection connection = tube.getRemoteConnection(travelDirection);
		return connection == null
			? getAdjacentRenderOffset(travelDirection, lerpFactor)
			: getRemoteItemRenderOffset(connection, travelDirection, tube.getBlockPos(), lerpFactor);
	}
	
	public static double getItemRenderScale(TubeBlockEntity tube, Direction travelDirection, float lerpFactor)
	{
		@Nullable RemoteConnection connection = tube.getRemoteConnection(travelDirection);
		return connection == null
			? 1D
			: getRemoteItemRenderScale(connection, travelDirection, tube.getBlockPos(), lerpFactor);
	}
	
	/**
	 * Get the render offset to render an item travelling to a remote tube
	 * @param connection
	 * @param travelDirection
	 * @param fromPos
	 * @param lerpFactor
	 * @return
	 */
	public static Vec3 getRemoteItemRenderOffset(RemoteConnection connection, Direction travelDirection, BlockPos fromPos, float lerpFactor)
	{
		Vec3 startVec = Vec3.atCenterOf(fromPos);
		BlockPos endPos = connection.toPos;
		Vec3 endVec = Vec3.atCenterOf(endPos);
		Direction endSide = connection.toSide;
		Vec3 startSideVec = RaytraceHelper.getTubeSideCenter(fromPos, travelDirection);
		Vec3 endSideVec = RaytraceHelper.getTubeSideCenter(endPos, endSide);
		// render item exiting origin tube
		if (lerpFactor < 0.25F)
		{
			Vec3 sideOffset = startSideVec.subtract(startVec);
			float subLerp = lerpFactor / 0.25F;
			double x = Mth.lerp(subLerp, 0, sideOffset.x);
			double y = Mth.lerp(subLerp, 0, sideOffset.y);
			double z = Mth.lerp(subLerp, 0, sideOffset.z);
			return new Vec3(x,y,z);
		}
		else if (lerpFactor < 0.75F) // render item between tubes
		{
			float subLerp = (lerpFactor - 0.25F) / 0.5F; // lerp with 0% = 0.25, 100% = 0.75
			double x = Mth.lerp(subLerp, startSideVec.x, endSideVec.x);
			double y = Mth.lerp(subLerp, startSideVec.y, endSideVec.y);
			double z = Mth.lerp(subLerp, startSideVec.z, endSideVec.z);
			// these values are in absolute coords
			// want to make them local to the renderer
			return new Vec3(x - startVec.x, y - startVec.y, z - startVec.z);
			
		}
		else // render item entering destination tube
		{
			float subLerp = (lerpFactor - 0.75F) / 0.25F; // lerp with 0% = 0.75, 100% = 1.0
			double x = Mth.lerp(subLerp, endSideVec.x, endVec.x);
			double y = Mth.lerp(subLerp, endSideVec.y, endVec.y);
			double z = Mth.lerp(subLerp, endSideVec.z, endVec.z);
			// these values are in absolute coords
			// want to make them local to the renderer
			return new Vec3(x - startVec.x, y - startVec.y, z - startVec.z);
		}
	}
	
	public static double getRemoteItemRenderScale(RemoteConnection connection, Direction travelDirection, BlockPos fromPos, float lerpFactor)
	{
		Direction remoteFace = connection.toSide;
		BlockPos remotePos = connection.toPos;
		double smallestScale = Math.min(getRemoteItemRenderScale(travelDirection, fromPos, remotePos), getRemoteItemRenderScale(remoteFace, remotePos, fromPos));
		if (lerpFactor < 0.25F)
		{
			double subLerp = (lerpFactor - 0.25F) / 0.25F;
			return Mth.lerp(subLerp, 1F, smallestScale);
		}
		else if (lerpFactor < 0.75F)
		{
			return smallestScale;
		}
		else
		{
			double subLerp = (lerpFactor - 0.75F) / 0.25F;
			return Mth.lerp(subLerp, smallestScale, 1F);
		}
	}
	
	public static double getRemoteItemRenderScale(Direction startSide, BlockPos startPos, BlockPos toPos)
	{
		Vec3i dist = toPos.subtract(startPos);
		Axis travelAxis = startSide.getAxis();
		Axis[] orthagonalAxes = DirectionTransformer.ORTHAGONAL_AXES[travelAxis.ordinal()];
		double parallelDistance = startSide.getAxis().choose(dist.getX(), dist.getY(), dist.getZ());
		double parallelDistanceSquared = parallelDistance * parallelDistance;
		double orthagonalDistanceSquared = 0;
		int axisCount = orthagonalAxes.length;
		for (int i=0; i<axisCount; i++)
		{
			int orthagonalDist = orthagonalAxes[i].choose(dist.getX(), dist.getY(), dist.getZ());
			orthagonalDistanceSquared += (orthagonalDist*orthagonalDist);
		}
		
		return Math.exp(- (orthagonalDistanceSquared / parallelDistanceSquared));
	}
	
	/**
	 * Get the render offset to render an item travelling to an adjacent tube
	 * @param travelDirection
	 * @param lerpFactor
	 * @return
	 */
	public static Vec3 getAdjacentRenderOffset(Direction travelDirection, float lerpFactor)
	{
		double xEnd = travelDirection.getStepX();
		double yEnd = travelDirection.getStepY();
		double zEnd = travelDirection.getStepZ();
		double xLerp = Mth.lerp(lerpFactor, 0, xEnd);
		double yLerp = Mth.lerp(lerpFactor, 0, yEnd);
		double zLerp = Mth.lerp(lerpFactor, 0, zEnd);
		return new Vec3(xLerp, yLerp, zLerp);
	}
	

	public void renderLongTubes(TubeBlockEntity tube, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int combinedLight, int combinedOverlay)
	{
		Level world = tube.getLevel();
		BlockPos startPos = tube.getBlockPos();
		Block block = tube.getBlockState().getBlock();
		if (block instanceof TubeBlock tubeBlock)
		{
			for (Map.Entry<Direction, RemoteConnection> entry : tube.getRemoteConnections().entrySet())
			{
				RemoteConnection connection = entry.getValue();
				// connections are stored in both tubes but only one tube should render each connection
				if (connection.isPrimary)
				{

					BlockPos endPos = connection.toPos;
					Direction startFace = entry.getKey();
					Direction endFace = connection.toSide;
					
					TubeQuadRenderer.renderQuads(world, partialTicks, startPos, endPos, startFace, endFace, matrix, buffer, tubeBlock);
				
				}
			}
			
			@SuppressWarnings("resource")
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null)
			{
				for (InteractionHand hand : InteractionHand.values())
				{
					ItemStack stack = player.getItemInHand(hand);
					if (stack.getItem() instanceof PliersItem)
					{
						@Nullable BlockSide plieredTube = PliersItem.getPlieredTube(stack);
						if (plieredTube != null)
						{
							BlockPos posOfLastTubeOfPlayer = plieredTube.pos();
							Direction sideOfLastTubeOfPlayer = plieredTube.direction();
							if (posOfLastTubeOfPlayer.equals(tube.getBlockPos()))
							{

								EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
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
									double fov = renderManager.options.fov().get().doubleValue();
									fov = fov / 100.0D;
									Vec3 handVector = new Vec3(-0.14 + handSideID * -0.36D * fov, -0.12 + -0.045D * fov, 0.4D);
									handVector = handVector.xRot(-Mth.lerp(partialTicks, player.xRotO, player.getXRot()) * ((float) Math.PI / 180F));
									handVector = handVector.yRot(-Mth.lerp(partialTicks, player.yHeadRotO, player.yHeadRot) * ((float) Math.PI / 180F));
									handVector = handVector.yRot(swingZ * 0.5F);
									handVector = handVector.xRot(-swingZ * 0.7F);
									handX = Mth.lerp(partialTicks, player.xo, player.getX()) + handVector.x;
									handY = Mth.lerp(partialTicks, player.yo, player.getY()) + handVector.y + 0.0F;
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
								Vec3 startVec = RaytraceHelper.getTubeSideCenter(posOfLastTubeOfPlayer, sideOfLastTubeOfPlayer);
								Vec3 endVec = renderPlayerVec;
								Vec3[] points = RaytraceHelper.getInterpolatedPoints(startVec, endVec);
								for (Vec3 point : points)
								{
									world.addParticle(ParticleTypes.CURRENT_DOWN, point.x, point.y, point.z, 0D, 0D, 0D);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean shouldRenderOffScreen(TubeBlockEntity te)
	{
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(TubeBlockEntity te)
	{
		return te.renderAABB;
	}
}
