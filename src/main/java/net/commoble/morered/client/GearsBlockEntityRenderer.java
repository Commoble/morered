package net.commoble.morered.client;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.client.GearsBlockEntityRenderer.GearsRenderState;
import net.commoble.morered.util.Lambdas;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record GearsBlockEntityRenderer(ItemModelResolver resolver) implements BlockEntityRenderer<GenericBlockEntity, GearsRenderState>
{
	
	public static class GearsSideRenderState
	{
		public final ItemStackRenderState itemState = new ItemStackRenderState();
		public float radians = 0F;
		public float parityFix = 0F;
		
		public void clear()
		{
			this.itemState.clear();
			this.radians = 0F;
			this.parityFix = 0F;
		}
	}

	public static class GearsRenderState extends BlockEntityRenderState
	{
		public final Map<Direction, GearsSideRenderState> sideStates = Util.makeEnumMap(Direction.class, Lambdas.ignoreInput(GearsSideRenderState::new));
		public double manhattanZFightOffset = 0D;
		
		public void clear()
		{
			this.manhattanZFightOffset = 0D;
			for (Direction dir : Direction.values())
			{
				this.sideStates.get(dir).clear();
			}
		}
	}
	
	public static GearsBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new GearsBlockEntityRenderer(context.itemModelResolver());
	}
	
	public GearsRenderState createRenderState()
	{
		return new GearsRenderState();
	}

	@Override
	public void extractRenderState(GenericBlockEntity be, GearsRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(be, renderState, partialTicks, camera, overlay);
		@Nullable Map<Direction, ItemStack> items = be.get(MoreRed.GEARS_DATA_COMPONENT.get());
		if (items == null || items.isEmpty())
		{
			// erase the item renderstates
			for (Direction dir : Direction.values())
			{
				renderState.sideStates.get(dir).clear();
			}
			renderState.manhattanZFightOffset = 0D;
		}
		else
		{
			Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
			BlockState state = be.getBlockState();
			@SuppressWarnings("resource")
			Level level = Minecraft.getInstance().level;
			int gameTimeTicks = MechanicalState.getMachineTicks(level);
			float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
			BlockPos pos = be.getBlockPos();
			int manhattanOffset = (int)((long)pos.getX() + (long)pos.getY() + (long)pos.getZ() % 6L);
			if (manhattanOffset < 0)
				manhattanOffset = manhattanOffset + 6;
			renderState.manhattanZFightOffset = manhattanOffset * 0.0001D;
			for (Direction facing : Direction.values())
			{
				GearsSideRenderState sideState = renderState.sideStates.get(facing);
				if (state.getValue(FaceSegmentBlock.getProperty(facing)))
				{
					ItemStack stack = items.get(facing);
					if (stack == null)
					{
						sideState.clear();
					}
					else
					{
						MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofSide(facing), MechanicalState.ZERO);
						float radiansPerSecond = (float)mechanicalState.angularVelocity();
						float radians = radiansPerSecond * seconds;
						radians += Mth.PI * 0.0625F * (manhattanOffset + facing.ordinal() / 6F);
						sideState.radians = radians;
						sideState.parityFix = facing.ordinal() % 2 == 0 ? 1F : -1F;
						this.resolver.updateForTopItem(
							sideState.itemState,
							stack,
							ItemDisplayContext.NONE,
							level,
							null,
							(int)pos.asLong());
							
					}
				}
			}
		}
	}

	@Override
	public void submit(GearsRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		poseStack.pushPose();
		// item renderer renders the center of the item model at 0,0,0 in the cube
		poseStack.translate(0.5D,0.5D,0.5D);
		for (Direction facing : Direction.values())
		{
			GearsSideRenderState sideState = renderState.sideStates.get(facing);
			if (sideState.itemState.isEmpty())
				continue;
			
			poseStack.pushPose();
			switch(facing)
			{
				case DOWN: poseStack.mulPose(Axis.XN.rotationDegrees(90F));break;
				case UP: poseStack.mulPose(Axis.XP.rotationDegrees(90));break;
				case SOUTH: poseStack.mulPose(Axis.YP.rotationDegrees(180F));break;
				case WEST: poseStack.mulPose(Axis.YP.rotationDegrees(90F));break;
				case EAST: poseStack.mulPose(Axis.YP.rotationDegrees(270F));break;
				default:
			}
			poseStack.translate(0D,0D,-(renderState.manhattanZFightOffset + facing.ordinal() * 0.00001D));
			// because of the rotation above, the positive directions (up,south,east) are now about to rotate in the wrong direction
			// invert the rotation if needed
			poseStack.mulPose(Axis.ZP.rotation(sideState.radians * sideState.parityFix));
			sideState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
