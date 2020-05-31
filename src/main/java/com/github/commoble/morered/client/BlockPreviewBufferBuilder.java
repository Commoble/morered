package com.github.commoble.morered.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.Vector4f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3i;

public class BlockPreviewBufferBuilder extends BufferBuilder
{
	public static final float PREVIEW_TRANSPARENCY = 0.5F;
	public static final int BUFFER_SIZE = 262144; // same size as translucent block rendertype
	
	public static final BlockPreviewBufferBuilder INSTANCE = new BlockPreviewBufferBuilder(BUFFER_SIZE);

	public BlockPreviewBufferBuilder(int bufferSizeIn)
	{
		super(bufferSizeIn);
	}

	@Override
	public void addQuad(MatrixStack.Entry matrixEntryIn, BakedQuad quadIn, float[] colorMuls, float redIn, float greenIn, float blueIn, int[] combinedLightsIn,
		int combinedOverlayIn, boolean mulColor)
	{
		int[] aint = quadIn.getVertexData();
		Vec3i vec3i = quadIn.getFace().getDirectionVec();
		Vector3f vector3f = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
		Matrix4f matrix4f = matrixEntryIn.getMatrix();
		vector3f.transform(matrixEntryIn.getNormal());
		int i = 8;
		int j = aint.length / 8;

		try (MemoryStack memorystack = MemoryStack.stackPush())
		{
			ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormats.BLOCK.getSize());
			IntBuffer intbuffer = bytebuffer.asIntBuffer();

			for (int k = 0; k < j; ++k)
			{
				((Buffer) intbuffer).clear();
				intbuffer.put(aint, k * 8, 8);
				float f = bytebuffer.getFloat(0);
				float f1 = bytebuffer.getFloat(4);
				float f2 = bytebuffer.getFloat(8);
				float f3;
				float f4;
				float f5;
				if (mulColor)
				{
					float f6 = (bytebuffer.get(12) & 255) / 255.0F;
					float f7 = (bytebuffer.get(13) & 255) / 255.0F;
					float f8 = (bytebuffer.get(14) & 255) / 255.0F;
					f3 = f6 * colorMuls[k] * redIn;
					f4 = f7 * colorMuls[k] * greenIn;
					f5 = f8 * colorMuls[k] * blueIn;
				}
				else
				{
					f3 = colorMuls[k] * redIn;
					f4 = colorMuls[k] * greenIn;
					f5 = colorMuls[k] * blueIn;
				}

				int l = this.applyBakedLighting(combinedLightsIn[k], bytebuffer);
				float f9 = bytebuffer.getFloat(16);
				float f10 = bytebuffer.getFloat(20);
				Vector4f vector4f = new Vector4f(f, f1, f2, 1.0F);
				vector4f.transform(matrix4f);
				this.applyBakedNormals(vector3f, bytebuffer, matrixEntryIn.getNormal());
				this.addVertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), f3, f4, f5, PREVIEW_TRANSPARENCY, f9, f10, combinedOverlayIn, l, vector3f.getX(), vector3f.getY(),
					vector3f.getZ());
			}
		}
	}
}
