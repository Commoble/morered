package net.commoble.morered.util;

import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.commoble.morered.PlayerData;
import net.commoble.morered.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class WorldHelper
{
	/**
	 * Distributes itemstack to target inventory and returns the uninserted portion of the stack
	 * @param handler
	 * @param stack
	 * @param context
	 * @return portion of ItemStack not inserted
	 */
	public static ItemStack insertItemStacked(ResourceHandler<ItemResource> handler, ItemStack stack, @Nullable TransactionContext context)
	{
		int oldCount = stack.getCount();
		int inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), oldCount, context);
		return stack.copyWithCount(oldCount - inserted);
	}
	
	public static ItemStack insertItemStackedImmediate(ResourceHandler<ItemResource> handler, ItemStack stack)
	{
		return insertItemStacked(handler, stack, null);
	}
	
	public static void ejectItemResources(Level level, BlockPos fromPos, @Nullable Direction outputDir, ItemResource resource, int amount)
	{
		int remainingAmount = amount;
		while (remainingAmount > 0)
		{
			int stackSize = Math.min(remainingAmount, resource.getMaxStackSize());
			ItemStack stack = resource.toStack(stackSize);
			ejectItemstack(level, fromPos, outputDir, stack);
			remainingAmount = Math.max(remainingAmount - stackSize, 0);
		}
	}
	
	public static void ejectItemstack(Level world, BlockPos from_pos, @Nullable Direction output_dir, ItemStack stack)
	{
		// if there is room in front of the shunt, eject items there
		double x,y,z,xVel,yVel,zVel, xOff, yOff, zOff;
		BlockPos outputPos;
		if (output_dir != null)
		{
			outputPos = from_pos.relative(output_dir);
			xOff = output_dir.getStepX();
			yOff = output_dir.getStepY();
			zOff = output_dir.getStepZ();
		}
		else
		{
			outputPos = from_pos;
			xOff = 0D;
			yOff = 0D;
			zOff = 0D;
		}
		if (!world.getBlockState(outputPos).isCollisionShapeFullBlock(world, outputPos))
		{
			x = from_pos.getX() + 0.5D + xOff*0.75D;
			y = from_pos.getY() + 0.25D + yOff*0.75D;
			z = from_pos.getZ() + 0.5D + zOff*0.75D;
			xVel = xOff * 0.1D;
			yVel = yOff * 0.1D;
			zVel = zOff * 0.1D;
		}
		else	// otherwise just eject items inside the shunt
		{
			x = from_pos.getX() + 0.5D;
			y = from_pos.getY() + 0.5D;
			z = from_pos.getZ() + 0.5D;
			xVel = 0D;
			yVel = 0D;
			zVel = 0D;
		}
		ItemEntity itementity = new ItemEntity(world, x, y, z, stack);
        itementity.setDefaultPickUpDelay();
        itementity.setDeltaMovement(xVel,yVel,zVel);
        world.addFreshEntity(itementity);
	}
	
	public static boolean doesItemHandlerHaveAnyExtractableItems(ResourceHandler<ItemResource> handler, Predicate<Item> doesCallerWantItem)
	{
		return IntStream.range(0,handler.size())
			.mapToObj(i -> handler.getResource(i))
			.anyMatch(resource -> !resource.isEmpty() && doesCallerWantItem.test(resource.getItem()));
	}

	public static Direction getBlockFacingForPlacement(BlockPlaceContext context)
	{
		// if sprint is being held (i.e. ctrl by default), facing is based on the face of the block that was clicked on
		// otherwise, facing is based on the look vector of the player
		// holding sneak reverses the facing of the placement to the opposite face
		boolean isSprintKeyHeld;
		if (context.getLevel().isClientSide())	// client thread
		{
			isSprintKeyHeld = ClientProxy.getWasSprinting();
		}
		else	// server thread
		{
			isSprintKeyHeld = PlayerData.getSprinting(context.getPlayer().getUUID());
		}
				
		Direction placeDir = isSprintKeyHeld ? context.getClickedFace().getOpposite() : context.getNearestLookingDirection();
		placeDir = context.isSecondaryUseActive() ? placeDir : placeDir.getOpposite();	// is player sneaking
		return placeDir;		
	}
}
