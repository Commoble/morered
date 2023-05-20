package commoble.morered.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public abstract class ExtraRenderTypes extends RenderStateShard
{
	// same as the leash renderer but with texture
	public static final RenderType CABLE_RENDER_TYPE = RenderType.create("morered:cable", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false,
		RenderType.CompositeState.builder()
			.setShaderState(RENDERTYPE_TEXT_SHADER)
			.setTextureState(RenderType.BLOCK_SHEET)
			.setCullState(RenderType.NO_CULL)
			.setLightmapState(RenderType.LIGHTMAP)
			.createCompositeState(false));

	ExtraRenderTypes(String nameIn, Runnable setupTaskIn, Runnable clearTaskIn)
	{
		super(nameIn, setupTaskIn, clearTaskIn);
	}
}
