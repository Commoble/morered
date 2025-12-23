package net.commoble.morered.client;

import java.util.List;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;

public class ModelUtil
{	
	public static BlockModelWrapper wrapBlockModel(ItemModel.BakingContext context, Identifier id, ModelState modelState, List<ItemTintSource> tints)
	{
		ModelBaker modelbaker = context.blockModelBaker();
		ResolvedModel resolvedmodel = modelbaker.getModel(id);
		TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
		List<BakedQuad> list = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, modelState).getAll();
		ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
		var renderTypeGroup = resolvedmodel.getTopAdditionalProperties().getOptional(NeoForgeModelProperties.RENDER_TYPE);
		var renderType = renderTypeGroup == null ? BlockModelWrapper.detectRenderType(list) : RenderTypeHelper.detectItemModelRenderType(list, renderTypeGroup);
		return new BlockModelWrapper(tints, list, modelrenderproperties, renderType);
	}
}
