package net.commoble.morered.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.commoble.morered.transportation.TubeBlock;
import net.commoble.morered.util.DirectionTransformer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class TubeQuadRenderer
{
	public static final Map<ResourceLocation, Material> MATERIALS = new HashMap<>();
	
	public static void renderQuads(Level level, float partialTicks, BlockPos startPos, BlockPos endPos, Direction startFace, Direction endFace, PoseStack matrix, MultiBufferSource buffer, TubeBlock block)
	{
		@SuppressWarnings("deprecation")
		TextureAtlasSprite textureatlassprite = MATERIALS.computeIfAbsent(block.textureLocation, tex -> new Material(TextureAtlas.LOCATION_BLOCKS, tex)).sprite();
		
		// this Vec3 method converts (1,2,3) into (1D, 2D, 3D)
		Vec3 startVec = Vec3.atLowerCornerOf(startPos);
		Vec3 endVec = Vec3.atLowerCornerOf(endPos);
		
		Vec3[][] vertices = DirectionTransformer.getVertexPairs(startFace, endFace);
		Vec3 offsetToEndPos = endVec.subtract(startVec);
		
		VertexConsumer ivertexbuilder = buffer.getBuffer(Sheets.solidBlockSheet());
		float totalMinU = textureatlassprite.getU0();
		float totalMinV = textureatlassprite.getV0();
		float totalMaxU = textureatlassprite.getU1();
		float totalMaxV = textureatlassprite.getV1();
		float texWidth = totalMaxU - totalMinU;
		float texHeight = totalMaxV - totalMinV;
		float tubeStartX = ((6F / 16F) * texWidth) + totalMinU;
		float tubeStartY = totalMinV;
		float tubeWidth = (4F / 16F) * texWidth;
		float tubeHeight = (4F / 16F) * texHeight;
		float minU = tubeStartX;
		float minV = tubeStartY;
		float maxU = tubeStartX + tubeWidth;
		float maxV = tubeStartY + tubeHeight;
		
		int startLight = getPackedLight(level, startPos);
		int endLight = getPackedLight(level, endPos);

		for (int side=0; side<4; side++)
		{
			matrix.pushPose();
			
			int vertIndexA = side;
			int vertIndexB = (side+1)%4;
			Vec3 startVertexA = vertices[vertIndexA][0];
			Vec3 startVertexB = vertices[vertIndexB][0];
			Vec3 endVertexB = vertices[vertIndexB][1].add(offsetToEndPos);
			Vec3 endVertexA = vertices[vertIndexA][1].add(offsetToEndPos);

			float xA = (float) startVertexA.x + 0.5F;
			float xB = (float) startVertexB.x + 0.5F;
			float xC = (float) endVertexB.x + 0.5F;
			float xD = (float) endVertexA.x + 0.5F;
			float yA = (float) startVertexA.y + 0.5F;
			float yB = (float) startVertexB.y + 0.5F;
			float yC = (float) endVertexB.y + 0.5F;
			float yD = (float) endVertexA.y + 0.5F;
			float zA = (float) startVertexA.z + 0.5F;
			float zB = (float) startVertexB.z + 0.5F;
			float zC = (float) endVertexB.z + 0.5F;
			float zD = (float) endVertexA.z + 0.5F;

			PoseStack.Pose matrixEntry = matrix.last();
			
			// need to calculate normals so vertex can have sided lighting
			// to get the normal for a vertex v1 connected to v2 and v3,
			// we take the cross product (v2 - v1) x (v3 - v1)
			// for a given quad, all four vertices should have the same normal, so we only need to calculate one of them
			// and reverse it for the reverse quad
			
			Vec3 normal = startVertexB.subtract(startVertexA).cross((endVertexA.subtract(startVertexA))).normalize();
			Vec3 reverseNormal = normal.multiply(-1, -1, -1);

			putVertex(matrixEntry, ivertexbuilder, xA, yA, zA, minU, maxV, startLight, normal);
			putVertex(matrixEntry, ivertexbuilder, xB, yB, zB, maxU, maxV, startLight, normal);
			putVertex(matrixEntry, ivertexbuilder, xC, yC, zC, maxU, minV, endLight, normal);
			putVertex(matrixEntry, ivertexbuilder, xD, yD, zD, minU, minV, endLight, normal);
			
			// also add the vertices in reverse order so we render the insides of the tubes
			putVertex(matrixEntry, ivertexbuilder, xD, yD, zD, minU, minV, endLight, reverseNormal);
			putVertex(matrixEntry, ivertexbuilder, xC, yC, zC, maxU, minV, endLight, reverseNormal);
			putVertex(matrixEntry, ivertexbuilder, xB, yB, zB, maxU, maxV, startLight, reverseNormal);
			putVertex(matrixEntry, ivertexbuilder, xA, yA, zA, minU, maxV, startLight, reverseNormal);

			matrix.popPose();
		}
		
	}

	private static void putVertex(PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, float x, float y, float z, float texU, float texV, int packedLight, Vec3 normal)
	{
		bufferIn.addVertex(matrixEntryIn.pose(), x, y, z)
			.setColor(1F,1F,1F, 1F)
			.setUv(texU, texV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(packedLight)
			.setNormal(matrixEntryIn, (float)normal.x, (float)normal.y, (float)normal.z);
	}
	
	public static int getPackedLight(Level world, BlockPos pos)
	{
		int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
		int skyLight = world.getBrightness(LightLayer.SKY, pos);
		return LightTexture.pack(blockLight, skyLight);
	}
}
