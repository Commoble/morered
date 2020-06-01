package com.github.commoble.morered.gatecrafting_plinth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class GatecraftingPlinthBlock extends Block
{

	public GatecraftingPlinthBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTrace)
	{
		if (player instanceof ServerPlayerEntity)
		{
			IContainerProvider provider = GatecraftingContainer.getServerContainerProvider(pos);
			ITextComponent name = new TranslationTextComponent(this.getTranslationKey());
			INamedContainerProvider namedProvider = new SimpleNamedContainerProvider(provider, name);
			NetworkHooks.openGui((ServerPlayerEntity)player, namedProvider);
		}

		return ActionResultType.SUCCESS;
	}
}
