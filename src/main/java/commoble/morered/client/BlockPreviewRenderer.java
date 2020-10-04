package commoble.morered.client;

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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.world.IBlockDisplayReader;
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
	public static void renderBlockPreview(BlockPos pos, BlockState state, World world, Vector3d currentRenderPos, MatrixStack matrix, IRenderTypeBuffer renderTypeBuffer)
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
	public void renderQuadSmooth(IBlockDisplayReader world, BlockState state, BlockPos pos, IVertexBuilder buffer, MatrixStack.Entry matrixEntry, BakedQuad quadIn,
		float tintA, float tintB, float tintC, float tintD, int brightness0, int brightness1, int brightness2, int brightness3, int combinedOverlayIn)
	{
		float r=1F;
		float g=1F;
		float b=1F;
		if (quadIn.hasTintIndex())
		{
			int i = this.blockColors.getColor(state, world, pos, quadIn.getTintIndex());
			r = (i >> 16 & 255) / 255.0F;
			g = (i >> 8 & 255) / 255.0F;
			b = (i & 255) / 255.0F;
		}
		// FORGE: Apply diffuse lighting at render-time instead of baking it in
		if (quadIn.applyDiffuseLighting()) // better name: shouldApplyDiffuseLighting
		{
			// TODO this should be handled by the forge lighting pipeline
			float forgeLighting = net.minecraftforge.client.model.pipeline.LightUtil.diffuseLight(quadIn.getFace());
			r *= forgeLighting;
			g *= forgeLighting;
			b *= forgeLighting;
		}

		// use our method below instead of adding the quad in the usual manner
		addTransparentQuad(matrixEntry, quadIn, new float[] { tintA, tintB, tintC, tintD }, r, g, b, new int[] { brightness0, brightness1, brightness2, brightness3 },
			combinedOverlayIn, true, buffer);
	}
	
	// as IVertexBuilder::addQuad except when we add the vertex, we add an opacity float instead of 1.0F
	public static void addTransparentQuad(MatrixStack.Entry matrixEntry, BakedQuad quad, float[] colorMuls, float r, float g, float b, int[] vertexLights,
		int combinedOverlayIn, boolean mulColor, IVertexBuilder buffer)
	{
		int[] vertexData = quad.getVertexData();
		Vector3i faceVector3i = quad.getFace().getDirectionVec();
		Vector3f faceVector = new Vector3f(faceVector3i.getX(), faceVector3i.getY(), faceVector3i.getZ());
		Matrix4f matrix = matrixEntry.getMatrix();
		faceVector.transform(matrixEntry.getNormal());
		
		int vertexDataEntries = vertexData.length / 8;

		try (MemoryStack memorystack = MemoryStack.stackPush())
		{
			ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormats.BLOCK.getSize());
			IntBuffer intbuffer = bytebuffer.asIntBuffer();

			for (int vertexIndex = 0; vertexIndex < vertexDataEntries; ++vertexIndex)
			{
				((Buffer) intbuffer).clear();
				intbuffer.put(vertexData, vertexIndex * 8, 8);
				float x = bytebuffer.getFloat(0);
				float y = bytebuffer.getFloat(4);
				float z = bytebuffer.getFloat(8);
				float red = colorMuls[vertexIndex] * r;
				float green = colorMuls[vertexIndex] * g;
				float blue = colorMuls[vertexIndex] * b;
				
				
				if (mulColor)
				{
					float redMultiplier = (bytebuffer.get(12) & 255) / 255.0F;
					float greenMultiplier = (bytebuffer.get(13) & 255) / 255.0F;
					float blueMultiplier = (bytebuffer.get(14) & 255) / 255.0F;
					red = redMultiplier * red;
					green = greenMultiplier * green;
					blue = blueMultiplier * blue;
				}
				
				// this is the important part
				float ALPHA = ClientConfig.INSTANCE.previewPlacementOpacity.get().floatValue();

				int light = buffer.applyBakedLighting(vertexLights[vertexIndex], bytebuffer);
				float texU = bytebuffer.getFloat(16);
				float texV = bytebuffer.getFloat(20);
				Vector4f posVector = new Vector4f(x, y, z, 1.0F);
				posVector.transform(matrix);
				buffer.applyBakedNormals(faceVector, bytebuffer, matrixEntry.getNormal());
				buffer.addVertex(posVector.getX(), posVector.getY(), posVector.getZ(), red, green, blue, ALPHA, texU, texV,
					combinedOverlayIn, light, faceVector.getX(), faceVector.getY(), faceVector.getZ());
			}
		}
	}
}
