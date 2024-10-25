package net.commoble.morered.transportation;

import java.util.stream.IntStream;

import com.mojang.datafixers.util.Pair;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class OsmosisFilterBlockEntity extends FilterBlockEntity
{
	public static final BlockEntityTicker<OsmosisFilterBlockEntity> SERVER_TICKER = (level, pos, state, filter) -> filter.serverTick();
	
	public boolean checkedItemsThisTick = false;

	public OsmosisFilterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public OsmosisFilterBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().osmosisFilterEntity.get(), pos, state);
	}
	
	public boolean getCheckedItemsAndMarkChecked()
	{
		boolean checked = this.checkedItemsThisTick;
		this.checkedItemsThisTick = true;
		return checked;
	}

	protected void serverTick()
	{
		if (!this.level.isClientSide())
		{
			this.checkedItemsThisTick = false;
			if ((this.level.getGameTime() + this.hashCode()) % MoreRed.SERVERCONFIG.osmosisFilterTransferRate().get() == 0
				&& this.getBlockState().getValue(OsmosisFilterBlock.TRANSFERRING_ITEMS))
			{
				Direction filterOutputDirection = this.getBlockState().getValue(FilterBlock.FACING);
				Direction filterInputDirection = filterOutputDirection.getOpposite();
				IItemHandler extractableHandler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, this.worldPosition.relative(filterInputDirection), filterOutputDirection);
				boolean successfulTransfer = extractableHandler != null
					&& this.attemptExtractionAndReturnSuccess(this.getFirstValidItem(extractableHandler));
				if (!successfulTransfer)
				{	// set dormant if no items were found
					this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(OsmosisFilterBlock.TRANSFERRING_ITEMS, false));
				}
				else
				{
					this.level.playSound(null, this.worldPosition, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS,
						this.level.random.nextFloat()*0.1F, this.level.random.nextFloat());//this.world.rand.nextFloat()*0.01f + 0.005f);
				}
				
			}
		}
	}

	private boolean attemptExtractionAndReturnSuccess(ItemStack stack)
	{
		if (stack.getCount() > 0)
		{
			this.shuntingHandler.insertItem(0, stack, false);
			return true;
		}
		return false;
	}

	public ItemStack getFirstValidItem(IItemHandler inventory)
	{
		return IntStream.range(0, inventory.getSlots()).mapToObj(slotIndex -> Pair.of(slotIndex, inventory.getStackInSlot(slotIndex)))
			.filter(slot -> this.canItemPassThroughFilter(slot.getSecond())).findFirst().map(slot -> inventory.extractItem(slot.getFirst(), 1, false)).orElse(ItemStack.EMPTY);
	}
}
