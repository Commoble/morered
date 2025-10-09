package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.commoble.morered.client.TubeBlockEntityRenderer.TubeRenderState;
import net.commoble.morered.transportation.TubeBlockEntity.TubeConnectionRenderInfo;
import net.commoble.morered.util.DirectionTransformer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class TubeQuadRenderer
{
	public static void renderQuads(TubeRenderState renderState, TubeConnectionRenderInfo connection, Direction startFace, TextureAtlasSprite textureatlassprite, PoseStack poseStack, SubmitNodeCollector collector)
	{
		BlockPos startPos = renderState.blockPos;
		BlockPos endPos = connection.endPos;
		Direction endFace = connection.endFace;
		// this Vec3 method converts (1,2,3) into (1D, 2D, 3D)
		Vec3 startVec = Vec3.atLowerCornerOf(startPos);
		Vec3 endVec = Vec3.atLowerCornerOf(endPos);
		
		Vec3[][] vertices = DirectionTransformer.getVertexPairs(startFace, endFace);
		Vec3 offsetToEndPos = endVec.subtract(startVec);
		
//		VertexConsumer ivertexbuilder = buffer.getBuffer(Sheets.solidBlockSheet());
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
		
		int startLight = renderState.startLight;
		int endLight = connection.endLight;

		for (int side=0; side<4; side++)
		{
			poseStack.pushPose();
			
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
			
			// need to calculate normals so vertex can have sided lighting
			// to get the normal for a vertex v1 connected to v2 and v3,
			// we take the cross product (v2 - v1) x (v3 - v1)
			// for a given quad, all four vertices should have the same normal, so we only need to calculate one of them
			// and reverse it for the reverse quad
			
			Vec3 normal = startVertexB.subtract(startVertexA).cross((endVertexA.subtract(startVertexA))).normalize();
			Vec3 reverseNormal = normal.multiply(-1, -1, -1);

			collector.submitCustomGeometry(poseStack, Sheets.solidBlockSheet(), (pose,vertexBuilder) -> {
				putVertex(pose, vertexBuilder, xA, yA, zA, minU, maxV, startLight, normal);
				putVertex(pose, vertexBuilder, xB, yB, zB, maxU, maxV, startLight, normal);
				putVertex(pose, vertexBuilder, xC, yC, zC, maxU, minV, endLight, normal);
				putVertex(pose, vertexBuilder, xD, yD, zD, minU, minV, endLight, normal);
				
				// also add the vertices in reverse order so we render the insides of the tubes
				putVertex(pose, vertexBuilder, xD, yD, zD, minU, minV, endLight, reverseNormal);
				putVertex(pose, vertexBuilder, xC, yC, zC, maxU, minV, endLight, reverseNormal);
				putVertex(pose, vertexBuilder, xB, yB, zB, maxU, maxV, startLight, reverseNormal);
				putVertex(pose, vertexBuilder, xA, yA, zA, minU, maxV, startLight, reverseNormal);
			});

			poseStack.popPose();
		}
		
	}

	private static void putVertex(PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, float x, float y, float z, float texU, float texV, int packedLight, Vec3 normal)
	{
		bufferIn.addVertex(matrixEntryIn, x, y, z)
			.setColor(1F,1F,1F, 1F)
			.setUv(texU, texV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(packedLight)
			.setNormal(matrixEntryIn, (float)normal.x, (float)normal.y, (float)normal.z);
	}
}
