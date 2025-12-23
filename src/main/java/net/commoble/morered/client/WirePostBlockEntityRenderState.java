package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.commoble.morered.MoreRed;
import net.commoble.morered.wire_post.WirePostBlockEntity;
import net.commoble.morered.wire_post.WirePostBlockEntity.WirePostConnectionRenderInfo;
import net.commoble.morered.wire_post.WireSpoolItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class WirePostBlockEntityRenderState extends BlockEntityRenderState
{
	public Vec3 startVec = Vec3.ZERO;
	public int startBlockLight = 0;
	public int startSkyLight = 0;
	public int red = 0;
	public List<WirePostConnectionRenderInfo> connectionStates = new ArrayList<>();
	public Map<InteractionHand, WirePostConnectionRenderInfo> playerConnections = new HashMap<>();
	
	public void updateForBlockLight(WirePostBlockEntity post, float partialTicks)
	{
		this.update(post, partialTicks);
		Level level = post.getLevel();
		BlockPos startPos = post.getBlockPos();
		this.startBlockLight = level.getBrightness(LightLayer.BLOCK, startPos);
		this.startSkyLight = level.getBrightness(LightLayer.SKY, startPos);
		for (WirePostConnectionRenderInfo info : this.connectionStates)
		{
			info.updateForBlockLight(level);
		}
		for (WirePostConnectionRenderInfo info : this.playerConnections.values())
		{
			info.updateForBlockLight(level);
		}
	}
	
	public void updateForRed(WirePostBlockEntity post, float partialTicks)
	{
		this.update(post, partialTicks);
		Level level = post.getLevel();
		BlockPos startPos = post.getBlockPos();
		this.red = WirePostBlockEntity.getRed(level, startPos, post.getBlockState(), partialTicks);
		for (WirePostConnectionRenderInfo info : this.connectionStates)
		{
			info.updateForRed(level, partialTicks);
		}
		for (WirePostConnectionRenderInfo info : this.playerConnections.values())
		{
			info.updateForRed(level, partialTicks);
		}
	}
	
	public void update(WirePostBlockEntity post, float partialTicks)
	{
		BlockPos startPos = post.getBlockPos();
		this.startVec = Vec3.atCenterOf(startPos);
		this.connectionStates = post.getConnectionRenderInfos();
		this.playerConnections.clear();

		Player player = Minecraft.getInstance().player;
		for (InteractionHand hand : InteractionHand.values())
		{
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof WireSpoolItem)
			{
				BlockPos positionOfCurrentPostOfPlayer = stack.get(MoreRed.SPOOLED_POST_DATA_COMPONENT.get());
				if (positionOfCurrentPostOfPlayer != null)
				{
					EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

					if (positionOfCurrentPostOfPlayer.equals(startPos))
					{
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
						WirePostConnectionRenderInfo info = new WirePostConnectionRenderInfo(renderPlayerVec);
						this.playerConnections.put(hand, info);
					}
				}
			}
		}
	}
	
	
}
