package commoble.morered.client;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public abstract class ExtraRenderTypes extends RenderState
{
	// same as the leash renderer but with texture
	public static final RenderType CABLE_RENDER_TYPE = RenderType.makeType("morered:cable", DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP, 7, 256,
		RenderType.State.getBuilder()
			.texture(RenderType.BLOCK_SHEET)
			.cull(RenderType.CULL_DISABLED)
			.lightmap(RenderType.LIGHTMAP_ENABLED)
			.build(false));

	public ExtraRenderTypes(String nameIn, Runnable setupTaskIn, Runnable clearTaskIn)
	{
		super(nameIn, setupTaskIn, clearTaskIn);
	}
}
