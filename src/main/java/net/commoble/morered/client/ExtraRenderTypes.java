package net.commoble.morered.client;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public abstract class ExtraRenderTypes extends RenderStateShard
{
	// same as the leash renderer but with texture
	public static final RenderType CABLE_RENDER_TYPE = RenderType.create(
		"morered:cable",
		256,
		RenderPipelines.TEXT,
		RenderType.CompositeState.builder()
			.setTextureState(RenderType.BLOCK_SHEET)
			.setLightmapState(RenderType.LIGHTMAP)
			.createCompositeState(false));

	ExtraRenderTypes(String nameIn, Runnable setupTaskIn, Runnable clearTaskIn)
	{
		super(nameIn, setupTaskIn, clearTaskIn);
	}
}
