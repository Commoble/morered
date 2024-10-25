package net.commoble.morered.transportation;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LoaderSlot extends Slot
{
	public LoaderSlot(LoaderMenu container, int index, int xPosition, int yPosition)
	{
		super(new LoaderInventory(container), index, xPosition, yPosition);
	}

	static class LoaderInventory implements Container
	{
		private LoaderMenu container;

		public LoaderInventory(LoaderMenu container)
		{
			this.container = container;
		}

		@Override
		public void clearContent()
		{
			// NOPE
		}
		
		@Override
		public int getContainerSize()
		{
			return 1;
		}

		@Override
		public boolean isEmpty()
		{
			return true;
		}

		@Override
		public ItemStack getItem(int index)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int index, int count)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItemNoUpdate(int index)
		{
			return ItemStack.EMPTY;
		}

		@Override
		public void setItem(int index, ItemStack stack)
		{
			Level world = this.container.player.level();
			BlockPos pos = this.container.pos;
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();

			if (block instanceof LoaderBlock)
			{
				((LoaderBlock)block).insertItem(stack, world, pos, state);
			}
		}

		@Override
		public void setChanged()
		{
			// NOPE
		}

		@Override
		public boolean stillValid(Player player)
		{
			return true;
		}

		@Override
		public int countItem(Item itemIn)
		{
			return 0;
		}

		@Override
		public boolean hasAnyOf(Set<Item> set)
		{
			return false;
		}
	}
}
