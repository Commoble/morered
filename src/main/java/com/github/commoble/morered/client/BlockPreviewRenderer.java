package com.github.commoble.morered.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.Vector4f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ILightReader;
import net.minecraft.world.World;

/**
 * Wrapper class around the vanilla block renderer so we don't have to copy five
 * functions verbatim
 **/
public class BlockPreviewRenderer extends BlockModelRenderer
{
	private static BlockPreviewRenderer INSTANCE;
	
	public static BlockPreviewRenderer getInstance(BlockModelRenderer baseRenderer)
	{
		if (INSTANCE == null || INSTANCE.blockColors != baseRenderer.blockColors)
		{
			INSTANCE = new BlockPreviewRenderer(baseRenderer);
		}
		
		return INSTANCE;
	}
	public BlockPreviewRenderer(BlockModelRenderer baseRenderer)
	{
		super(baseRenderer.blockColors);
	}

	// invoked from the DrawHighlightEvent.HighlightBlock event
	public static void renderBlockPreview(BlockPos pos, BlockState state, World world, Vec3d currentRenderPos, MatrixStack matrix, IRenderTypeBuffer renderTypeBuffer)
	{
		matrix.push();
	
		// the current position of the matrix stack is the position of the player's
		// viewport (the head, essentially)
		// we want to move it to the correct position to render the block at
		double offsetX = pos.getX() - currentRenderPos.getX();
		double offsetY = pos.getY() - currentRenderPos.getY();
		double offsetZ = pos.getZ() - currentRenderPos.getZ();
		matrix.translate(offsetX, offsetY, offsetZ);
	
		BlockRendererDispatcher blockDispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockModelRenderer renderer = getInstance(blockDispatcher.getBlockModelRenderer());
		renderer.renderModel(
			world,
			blockDispatcher.getModelForState(state),
			state,
			pos,
			matrix,
			renderTypeBuffer.getBuffer(RenderType.getTranslucent()),
			false,
			world.rand,
			state.getPositionRandom(pos),
			OverlayTexture.NO_OVERLAY,
			net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
	
		matrix.pop();
	}

	@Override
	public void renderQuadSmooth(ILightReader blockAccessIn, BlockState stateIn, BlockPos posIn, IVertexBuilder buffer, MatrixStack.Entry matrixEntry, BakedQuad quadIn,
		float colorMul0, float colorMul1, float colorMul2, float colorMul3, int brightness0, int brightness1, int brightness2, int brightness3, int combinedOverlayIn)
	{
		float f;
		float f1;
		float f2;
		if (quadIn.hasTintIndex())
		{
			int i = this.blockColors.getColor(stateIn, blockAccessIn, posIn, quadIn.getTintIndex());
			f = (i >> 16 & 255) / 255.0F;
			f1 = (i >> 8 & 255) / 255.0F;
			f2 = (i & 255) / 255.0F;
		}
		else
		{
			f = 1.0F;
			f1 = 1.0F;
			f2 = 1.0F;
		}
		// FORGE: Apply diffuse lighting at render-time instead of baking it in
		if (quadIn.shouldApplyDiffuseLighting())
		{
			// TODO this should be handled by the forge lighting pipeline
			float l = net.minecraftforge.client.model.pipeline.LightUtil.diffuseLight(quadIn.getFace());
			f *= l;
			f1 *= l;
			f2 *= l;
		}

		addTransparentQuad(matrixEntry, quadIn, new float[] { colorMul0, colorMul1, colorMul2, colorMul3 }, f, f1, f2, new int[] { brightness0, brightness1, brightness2, brightness3 },
			combinedOverlayIn, true, buffer);
	}
	
	// as IVertexBuilder::addQuad except when we add the vertex, we add an opacity float instead of 1.0F
	public static void addTransparentQuad(MatrixStack.Entry matrixEntryIn, BakedQuad quadIn, float[] colorMuls, float redIn, float greenIn, float blueIn, int[] combinedLightsIn,
		int combinedOverlayIn, boolean mulColor, IVertexBuilder buffer)
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

				int l = buffer.applyBakedLighting(combinedLightsIn[k], bytebuffer);
				float f9 = bytebuffer.getFloat(16);
				float f10 = bytebuffer.getFloat(20);
				Vector4f vector4f = new Vector4f(f, f1, f2, 1.0F);
				vector4f.transform(matrix4f);
				buffer.applyBakedNormals(vector3f, bytebuffer, matrixEntryIn.getNormal());
				buffer.addVertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), f3, f4, f5, ClientConfig.INSTANCE.previewPlacementOpacity.get().floatValue(), f9, f10,
					combinedOverlayIn, l, vector3f.getX(), vector3f.getY(), vector3f.getZ());
			}
		}
	}
}
