package net.commoble.morered.transportation;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

public class MultiFilterBlockEntity extends AbstractFilterBlockEntity
{
	public final SetItemHandler inventory = new SetItemHandler(27) {
		@Override
		protected void onContentsChanged(int slot, ItemStack oldStack)
		{
			super.onContentsChanged(slot, oldStack);
			MultiFilterBlockEntity.this.setChanged();
		}
	};
	public final ResourceHandler<ItemResource> shuntingHandler = new FilterShuntingItemHandler(this); 
	
	public MultiFilterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public MultiFilterBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.MULTIFILTER_BLOCK_ENTITY.get(), pos, state);
	}

	@Nullable
	public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side)
	{
		if (this.getBlockState().hasProperty(DirectionalBlock.FACING))
		{
			Direction outputDir = this.getBlockState().getValue(DirectionalBlock.FACING);
			if (side != outputDir)
			{
				return this.shuntingHandler;
			}
		}
		return null;
	}

	@Override
	public boolean canItemPassThroughFilter(Item item)
	{
		return this.inventory.getSet().contains(item);
	}

	@Override
	public void dropItems()
	{
		int slots = this.inventory.size();
		BlockPos pos = this.getBlockPos();
		for (int i=0; i<slots; i++)
		{
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), ItemUtil.getStack(this.inventory, i));
		}
	}

	@Override
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		input.child(INV_KEY).ifPresent(this.inventory::deserialize);
	}

	@Override
	protected void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		this.inventory.serialize(output.child(INV_KEY));
	}
}
