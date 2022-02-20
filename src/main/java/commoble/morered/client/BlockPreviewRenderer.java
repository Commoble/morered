package commoble.morered.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import commoble.morered.foundation.config.AllConfigs;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Wrapper class around the vanilla block renderer so we don't have to copy five
 * functions verbatim
 **/
public class BlockPreviewRenderer {
//	private static BlockPreviewRenderer INSTANCE;

//	public static BlockPreviewRenderer getInstance(ModelBlockRenderer baseRenderer)
//	{
//		if (INSTANCE == null || INSTANCE.blockColors != baseRenderer.blockColors)
//		{
//			INSTANCE = new BlockPreviewRenderer(baseRenderer);
//		}
//
//		return INSTANCE;
//	}

    public BlockPreviewRenderer() {
//		super(baseRenderer.blockColors);
    }

    // invoked from the DrawHighlightEvent.HighlightBlock event
    public static void renderBlockPreview(BlockPos pos, BlockState state, Level world, Vec3 currentRenderPos,
                                          PoseStack matrix, MultiBufferSource renderTypeBuffer) {
        matrix.pushPose();

        // the current position of the matrix stack is the position of the player's
        // viewport (the head, essentially)
        // we want to move it to the correct position to render the block at
        double offsetX = pos.getX() - currentRenderPos.x();
        double offsetY = pos.getY() - currentRenderPos.y();
        double offsetZ = pos.getZ() - currentRenderPos.z();
        matrix.translate(offsetX, offsetY, offsetZ);

        BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
//		BlockModelRenderer renderer = getInstance(blockDispatcher.getModelRenderer());
//		renderer.renderModel(
//			world,
//			blockDispatcher.getBlockModel(state),
//			state,
//			pos,
//			matrix,
//			renderTypeBuffer.getBuffer(RenderType.translucent()),
//			false,
//			world.random,
//			state.getSeed(pos),
//			OverlayTexture.NO_OVERLAY,
//			net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
        renderModel(matrix.last(),
                renderTypeBuffer.getBuffer(RenderType.translucent()),
                state,
                blockDispatcher.getBlockModel(state),
                1f, 1f, 1f,
                LevelRenderer.getLightColor(world, pos),
                OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.EmptyModelData.INSTANCE
        );

        matrix.popPose();
    }

    public static void renderModel(PoseStack.Pose entry, VertexConsumer vb, BlockState state, BakedModel model,
                                   float tintA, float tintB, float tintC, int brightness, int combinedOverlayIn,
                                   net.minecraftforge.client.model.data.IModelData modelData) {
        Random random = new Random();

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
        }

        random.setSeed(42L);

    }

    public static void renderQuad(PoseStack.Pose entry, VertexConsumer vb, BlockState state, BakedModel model,
                                  float tintA, float tintB, float tintC, List<BakedQuad> quads, int brightness,
                                  int combinedOverlayIn) {
        float alpha = (float) AllConfigs.CLIENT.previewPlacementOpacity.getD();

        for (BakedQuad bakedQuad : quads) {
            float f1, f2, f3;
            if (bakedQuad.isTinted()) {
                f1 = Mth.clamp(tintA, 0f, 1f);
                f2 = Mth.clamp(tintB, 0f, 1f);
                f3 = Mth.clamp(tintC, 0f, 1f);
            } else {
                float forgeLighting =
                        net.minecraftforge.client.model.pipeline.LightUtil.diffuseLight(bakedQuad.getDirection());
                f1 = forgeLighting;
                f2 = forgeLighting;
                f3 = forgeLighting;
            }

            quad(alpha, vb, entry, bakedQuad, new float[]{1f, 1f, 1f, 1f}, f1, f2, f3,
                    new int[]{brightness, brightness, brightness, brightness}, combinedOverlayIn);
        }
    }

    static void quad(float alpha, VertexConsumer vb, PoseStack.Pose p_227890_1_, BakedQuad p_227890_2_,
                     float[] p_227890_3_, float p_227890_4_, float p_227890_5_, float p_227890_6_, int[] p_227890_7_,
                     int p_227890_8_) {
        int[] aint = p_227890_2_.getVertices();
        Vec3i Vector3i = p_227890_2_.getDirection()
                .getNormal();
        Vector3f vector3f = new Vector3f((float) Vector3i.getX(), (float) Vector3i.getY(), (float) Vector3i.getZ());
        Matrix4f matrix4f = p_227890_1_.pose();
        vector3f.transform(p_227890_1_.normal());
        int vertexSize = DefaultVertexFormat.BLOCK.getIntegerSize();
        int j = aint.length / vertexSize;

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int k = 0; k < j; ++k) {
                ((Buffer) intbuffer).clear();
                intbuffer.put(aint, k * vertexSize, vertexSize);
                float f = bytebuffer.getFloat(0);
                float f1 = bytebuffer.getFloat(4);
                float f2 = bytebuffer.getFloat(8);
                float r;
                float g;
                float b;

                r = p_227890_3_[k] * p_227890_4_;
                g = p_227890_3_[k] * p_227890_5_;
                b = p_227890_3_[k] * p_227890_6_;

                int l = vb.applyBakedLighting(p_227890_7_[k], bytebuffer);
                float f9 = bytebuffer.getFloat(16);
                float f10 = bytebuffer.getFloat(20);
                Vector4f vector4f = new Vector4f(f, f1, f2, 1.0F);
                vector4f.transform(matrix4f);
                vb.applyBakedNormals(vector3f, bytebuffer, p_227890_1_.normal());
                vb.vertex(vector4f.x(), vector4f.y(), vector4f.z(), r, g, b, alpha, f9, f10, p_227890_8_,
                        l, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }

//	public void putQuadData(BlockAndTintGetter world, BlockState state, BlockPos pos, VertexConsumer buffer, PoseStack
//	.Pose matrixEntry, BakedQuad quadIn,
//							float tintA, float tintB, float tintC, float tintD, int brightness0, int brightness1, int
//							brightness2, int brightness3, int combinedOverlayIn)
//	{
//		float r=1F;
//		float g=1F;
//		float b=1F;
//		if (quadIn.isTinted()) {
//			int i = this.blockColors.getColor(state, world, pos, quadIn.getTintIndex());
//			r = (i >> 16 & 255) / 255.0F;
//			g = (i >> 8 & 255) / 255.0F;
//			b = (i & 255) / 255.0F;
//		}
//		// FORGE: Apply diffuse lighting at render-time instead of baking it in
//		if (quadIn.isShade()) // better name: shouldApplyDiffuseLighting
//		{
//			// TODO this should be handled by the forge lighting pipeline
//			float forgeLighting = net.minecraftforge.client.model.pipeline.LightUtil.diffuseLight(quadIn.getDirection
//			());
//			r *= forgeLighting;
//			g *= forgeLighting;
//			b *= forgeLighting;
//		}
//
//		// use our method below instead of adding the quad in the usual manner
//		addTransparentQuad(matrixEntry, quadIn, new float[] { tintA, tintB, tintC, tintD }, r, g, b, new int[] {
//		brightness0, brightness1, brightness2, brightness3 },
//			combinedOverlayIn, true, buffer);
//	}
//
//	// as IVertexBuilder::addQuad except when we add the vertex, we add an opacity float instead of 1.0F
//	public static void addTransparentQuad(PoseStack.Pose matrixEntry, BakedQuad quad, float[] colorMuls, float r,
//	float g, float b, int[] vertexLights,
//		int combinedOverlayIn, boolean mulColor, VertexConsumer buffer)
//	{
//		int[] vertexData = quad.getVertices();
//		Vec3i faceVector3i = quad.getDirection().getNormal();
//		Vector3f faceVector = new Vector3f(faceVector3i.getX(), faceVector3i.getY(), faceVector3i.getZ());
//		Matrix4f matrix = matrixEntry.pose();
//		faceVector.transform(matrixEntry.normal());
//
//		int vertexDataEntries = vertexData.length / 8;
//
//		try (MemoryStack memorystack = MemoryStack.stackPush())
//		{
//			ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
//			IntBuffer intbuffer = bytebuffer.asIntBuffer();
//
//			for (int vertexIndex = 0; vertexIndex < vertexDataEntries; ++vertexIndex)
//			{
//				((Buffer) intbuffer).clear();
//				intbuffer.put(vertexData, vertexIndex * 8, 8);
//				float x = bytebuffer.getFloat(0);
//				float y = bytebuffer.getFloat(4);
//				float z = bytebuffer.getFloat(8);
//				float red = colorMuls[vertexIndex] * r;
//				float green = colorMuls[vertexIndex] * g;
//				float blue = colorMuls[vertexIndex] * b;
//
//
//				if (mulColor)
//				{
//					float redMultiplier = (bytebuffer.get(12) & 255) / 255.0F;
//					float greenMultiplier = (bytebuffer.get(13) & 255) / 255.0F;
//					float blueMultiplier = (bytebuffer.get(14) & 255) / 255.0F;
//					red = redMultiplier * red;
//					green = greenMultiplier * green;
//					blue = blueMultiplier * blue;
//				}
//
//				// this is the important part
//				float ALPHA = ClientConfig.INSTANCE.previewPlacementOpacity.get().floatValue();
//
//				int light = buffer.applyBakedLighting(vertexLights[vertexIndex], bytebuffer);
//				float texU = bytebuffer.getFloat(16);
//				float texV = bytebuffer.getFloat(20);
//				Vector4f posVector = new Vector4f(x, y, z, 1.0F);
//				posVector.transform(matrix);
//				buffer.applyBakedNormals(faceVector, bytebuffer, matrixEntry.normal());
//				buffer.vertex(posVector.x(), posVector.y(), posVector.z(), red, green, blue, ALPHA, texU, texV,
//					combinedOverlayIn, light, faceVector.x(), faceVector.y(), faceVector.z());
//			}
//		}
//	}
}
