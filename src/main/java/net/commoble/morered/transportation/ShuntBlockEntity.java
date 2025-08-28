package net.commoble.morered.transportation;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class ShuntBlockEntity extends BlockEntity
{
	private final ShuntItemHandler outputHandler = new ShuntItemHandler(this, false);
	private final ShuntItemHandler inputHandler = new ShuntItemHandler(this, true);

	public ShuntBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
	{
		super(tileEntityTypeIn, pos, state);
	}

	public ShuntBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.SHUNT_BLOCK_ENTITY.get(), pos, state);
	}
	
	public IItemHandler getItemHandler(@Nullable Direction side)
	{
		Direction output_dir = this.getBlockState().getValue(ShuntBlock.FACING);
		return side == output_dir ? this.outputHandler : this.inputHandler;
	}
}
