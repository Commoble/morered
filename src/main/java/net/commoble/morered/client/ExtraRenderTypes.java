package net.commoble.morered.client;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

public final class ExtraRenderTypes
{
	private ExtraRenderTypes() {}
	
	// same as the leash renderer but with texture
	@SuppressWarnings("deprecation")
	public static final RenderType CABLE_RENDER_TYPE = RenderType.create(
		"morered:cable",
		RenderSetup.builder(RenderPipelines.TEXT)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
			.useLightmap()
			.createRenderSetup());
}
