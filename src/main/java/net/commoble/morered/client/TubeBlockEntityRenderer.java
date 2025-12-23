package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.MoreRed;
import net.commoble.morered.client.TubeBlockEntityRenderer.TubeRenderState;
import net.commoble.morered.transportation.ItemInTubeWrapper;
import net.commoble.morered.transportation.RaytraceHelper;
import net.commoble.morered.transportation.RemoteConnection;
import net.commoble.morered.transportation.TubeBlock;
import net.commoble.morered.transportation.TubeBlockEntity;
import net.commoble.morered.transportation.TubeBlockEntity.TubeConnectionRenderInfo;
import net.commoble.morered.util.DirectionTransformer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record TubeBlockEntityRenderer(ItemModelResolver resolver, MaterialSet materials) implements BlockEntityRenderer<TubeBlockEntity, TubeRenderState>
{
	public static final Map<Identifier,Material> MATERIALS = new HashMap<>();
	@SuppressWarnings("deprecation")
	public static Material getMaterial(Identifier textureId)
	{
		return MATERIALS.computeIfAbsent(textureId, id -> new Material(TextureAtlas.LOCATION_BLOCKS, id));
	}
	
	public static TubeBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new TubeBlockEntityRenderer(context.itemModelResolver(), context.materials());
	}
	
	public static class TubeRenderState extends BlockEntityRenderState
	{
		public List<ItemInTubeRenderState> itemInTubeRenderStates = new ArrayList<>();
		public Map<Direction, TubeConnectionRenderInfo> connections = new HashMap<>();
		public int startLight = 0;
		public Material material = getMaterial(MoreRed.TUBE_BLOCK.get().textureLocation);
	}
	
	public static record ItemInTubeRenderState(
		ItemStackRenderState itemState,
		List<Vec3> itemGroupOffsets,
		float scale
		)
	{
		public static void addToListFromWrapper(List<ItemInTubeRenderState> list, ItemModelResolver resolver, TubeBlockEntity tube, ItemInTubeWrapper wrapper, float partialTicks)
		{
			Direction nextMove = wrapper.remainingMoves.peek();
			if (nextMove == null)
				return;
			
			int seed = (int) tube.getBlockPos().asLong();
			Level level = tube.getLevel();
			ItemStackRenderState itemState = new ItemStackRenderState();
			resolver.updateForTopItem(itemState, wrapper.stack, ItemDisplayContext.GROUND, level, null, seed);
			ItemStack itemstack = wrapper.stack;
			
			Item item = itemstack.getItem();
			int renderSeed = itemstack.isEmpty() ? 187 : Item.getId(item) + itemstack.getDamageValue(); // the random is used to offset sub-items
			RandomSource random = RandomSource.create(renderSeed);
			
			int renderedItemCount = getModelCount(itemstack);
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
			float scale = remoteScale * 0.5F;

			List<Vec3> itemGroupOffsets = new ArrayList<>();
			for (int currentModelIndex = 0; currentModelIndex < renderedItemCount; ++currentModelIndex)
			{
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
				itemGroupOffsets.add(new Vec3(xTranslate, yTranslate, zTranslate));
			}
			
			list.add(new ItemInTubeRenderState(
				itemState,
				itemGroupOffsets,
				scale));
		}
	}

	@Override
	public TubeRenderState createRenderState()
	{
		return new TubeRenderState();
	}

	@Override
	public void extractRenderState(TubeBlockEntity tube, TubeRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(tube, renderState, partialTicks, camera, overlay);
		List<ItemInTubeRenderState> itemsInTube = new ArrayList<>();
		// render tick happens independently of regular ticks and often more frequently
		if (!tube.inventory.isEmpty())
		{
			for (ItemInTubeWrapper wrapper : tube.inventory)
			{
				ItemInTubeRenderState.addToListFromWrapper(itemsInTube, this.resolver, tube, wrapper, partialTicks);
			}
		}
		if (!tube.incomingWrapperBuffer.isEmpty())
		{
			for (ItemInTubeWrapper wrapper : tube.incomingWrapperBuffer)
			{
				ItemInTubeRenderState.addToListFromWrapper(itemsInTube, this.resolver, tube, wrapper, partialTicks);
			}
		}
		renderState.itemInTubeRenderStates = itemsInTube;
		if (tube.getBlockState().getBlock() instanceof TubeBlock tubeBlock)
		{
			renderState.material = getMaterial(tubeBlock.textureLocation);
		}
		else
		{
			renderState.material = getMaterial(MoreRed.TUBE_BLOCK.get().textureLocation);
		}
		renderState.connections = tube.getConnectionRenderInfos();
		for (TubeConnectionRenderInfo info : renderState.connections.values())
		{
			info.update(tube.getLevel());
		}
		Level level = tube.getLevel();
		BlockPos startPos = tube.getBlockPos();
		int blockLight = level.getBrightness(LightLayer.BLOCK, startPos);
		int skyLight = level.getBrightness(LightLayer.SKY, startPos);
		renderState.startLight = LightTexture.pack(blockLight, skyLight);
	}

	@Override
	public void submit(TubeRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		// render tick happens independently of regular ticks and often more frequently
		for (ItemInTubeRenderState itemInTubeRenderState : renderState.itemInTubeRenderStates)
		{
			this.renderWrapper(renderState, itemInTubeRenderState, poseStack, collector);
		}
		TextureAtlasSprite sprite = this.materials.get(renderState.material);
		for (var entry : renderState.connections.entrySet())
		{
			TubeQuadRenderer.renderQuads(renderState, entry.getValue(), entry.getKey(), sprite, poseStack, collector);
		}
	}

	// ** copied from entity ItemRenderer **//

	public static int getModelCount(ItemStack stack)
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
	public void renderWrapper(TubeRenderState renderState, ItemInTubeRenderState itemInTubeState, PoseStack poseStack, SubmitNodeCollector collector)
	{
		poseStack.pushPose();
		
		for (Vec3 offset : itemInTubeState.itemGroupOffsets)
		{
			poseStack.pushPose();
			poseStack.translate(offset.x, offset.y, offset.z);// aggregate is centered
			float scale = itemInTubeState.scale;
			poseStack.scale(scale, scale, scale);

			itemInTubeState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}

		poseStack.popPose();
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

	@Override
	public boolean shouldRenderOffScreen()
	{
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(TubeBlockEntity te)
	{
		return te.renderAABB;
	}
}
