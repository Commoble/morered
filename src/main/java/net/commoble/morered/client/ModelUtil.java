package net.commoble.morered.client;

import java.util.List;

import org.joml.Matrix4fc;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

public class ModelUtil
{	
	public static CuboidItemModelWrapper wrapBlockModel(ItemModel.BakingContext context, Identifier id, ModelState modelState, Matrix4fc transformMatrix, List<ItemTintSource> tints)
	{
		ModelBaker modelbaker = context.blockModelBaker();
		ResolvedModel resolvedmodel = modelbaker.getModel(id);
		TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
		QuadCollection quads = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, modelState);
		ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
		return new CuboidItemModelWrapper(tints, quads, modelrenderproperties, transformMatrix);
	}
}
