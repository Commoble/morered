package net.commoble.morered.bitwise_logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Receiver;
import net.commoble.exmachina.api.SignalGraphUpdateGameEvent;
import net.commoble.morered.plate_blocks.InputSide;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BitwiseGateBlockEntity extends BlockEntity
{
	public static final String OUTPUT = "power";
	
	protected int output = 0;
	
	protected Map<Channel, ToIntFunction<LevelReader>> supplierEndpoints = null;
	
	public abstract void resetUnusedReceivers(Collection<BitwiseListener> receivers);
	
	public BitwiseGateBlockEntity(BlockEntityType<? extends BitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public int getOutput()
	{
		return this.output;
	}
	
	public boolean hasOutput(int channel)
	{
		return (this.output & (1 << channel)) != 0;
	}
	
	public void setOutput(int output)
	{
		int oldOutput = this.output;
		this.output = output;
		if (oldOutput != this.output)
		{
			this.supplierEndpoints = null;
			this.setChanged();
			Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(this.getBlockState());
			SignalGraphUpdateGameEvent.scheduleSignalGraphUpdate(level, worldPosition.relative(primaryOutputDirection));
		}
	}

	protected Map<Channel, ToIntFunction<LevelReader>> getSupplierEndpoints()
	{
		if (this.supplierEndpoints == null)
		{
			this.supplierEndpoints = this.createSupplierEndpoints();
		}
		return this.supplierEndpoints;
	}

	private static final ToIntFunction<LevelReader> ON = level -> 15;
	private static final ToIntFunction<LevelReader> OFF = level -> 0;
	
	protected Map<Channel, ToIntFunction<LevelReader>> createSupplierEndpoints()
	{
		final int output = this.getOutput();
		Map<Channel, ToIntFunction<LevelReader>> map = new HashMap<>();
		for (DyeColor color : DyeColor.values())
		{
			final var power = (output & (1 << color.ordinal())) != 0
				? ON
				: OFF;
			map.put(Channel.single(color), power);
		}
		
		return map;
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		compound.putInt(OUTPUT, this.output);
	}
	
	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries);
		this.output = compound.getInt(OUTPUT);
	}
	
	public static record BitwiseListener(DyeColor color, InputSide inputSide, Receiver receiver) implements Receiver {
		@Override
		public void accept(LevelAccessor level, int power)
		{
			this.receiver.accept(level, power);
		}
	}
}
