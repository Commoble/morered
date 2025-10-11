package net.commoble.morered.transportation;

import com.google.common.base.Predicates;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
		this(MoreRed.OSMOSIS_FILTER_BLOCK_ENTITY.get(), pos, state);
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
				ResourceHandler<ItemResource> extractableHandler = this.level.getCapability(Capabilities.Item.BLOCK, this.worldPosition.relative(filterInputDirection), filterOutputDirection);
				boolean successfulTransfer = extractableHandler != null
					&& this.attemptExtractionAndReturnSuccess(extractableHandler);
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

	private boolean attemptExtractionAndReturnSuccess(ResourceHandler<ItemResource> extractableHandler)
	{
		boolean inserted = false;
		try(Transaction t = Transaction.openRoot())
		{
			ResourceStack<ItemResource> resourceStack = ResourceHandlerUtil.extractFirst(extractableHandler, Predicates.alwaysTrue(), 1, t);
			int amount = resourceStack.amount();
			if (amount > 0)
			{
				this.shuntingHandler.insert(resourceStack.resource(), amount, t);
				inserted = true;
			}
			t.commit();
		}
		return inserted;
	}
}
