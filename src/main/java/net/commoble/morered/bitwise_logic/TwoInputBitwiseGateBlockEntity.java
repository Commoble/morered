package net.commoble.morered.bitwise_logic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import net.commoble.morered.MoreRed;
import net.commoble.morered.future.Channel;
import net.commoble.morered.plate_blocks.InputSide;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TwoInputBitwiseGateBlockEntity extends BitwiseGateBlockEntity
{
	public static final String CLOCKWISE_INPUT = "clockwise_input";
	public static final String COUNTERCLOCKWISE_INPUT = "counterclockwise_input";
	
	protected int clockwiseInput = 0;
	protected int counterClockwiseInput = 0;
	protected Map<Channel, BiConsumer<LevelAccessor, Integer>> clockwiseReceiverEndpoints = Util.make(() -> {
		Map<Channel, BiConsumer<LevelAccessor, Integer>> map = new HashMap<>();
		for (DyeColor color : DyeColor.values())
		{
			final int channel = color.ordinal(); 
			map.put(Channel.single(color), (levelAccess, power) -> this.setClockwiseInputOnChannel(power > 0, channel));
		}
		return map;
	});
	protected Map<Channel, BiConsumer<LevelAccessor, Integer>> counterClockwiseReceiverEndpoints = Util.make(() -> {
		Map<Channel, BiConsumer<LevelAccessor, Integer>> map = new HashMap<>();
		for (DyeColor color : DyeColor.values())
		{
			final int channel = color.ordinal(); 
			map.put(Channel.single(color), (levelAccess, power) -> this.setCounterclockwiseInputOnChannel(power > 0, channel));
		}
		return map;
	});

	public TwoInputBitwiseGateBlockEntity(BlockEntityType<? extends TwoInputBitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public static TwoInputBitwiseGateBlockEntity create(BlockPos pos, BlockState state)
	{
		return new TwoInputBitwiseGateBlockEntity(MoreRed.get().twoInputBitwiseGateBeType.get(), pos, state);
	}
	
	public void setClockwiseInputOnChannel(boolean hasInput, int channel)
	{
		int newInput = this.clockwiseInput;
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setClockwiseInput(newInput, false);
	}
	
	public void setClockwiseInput(int input, boolean forceOutputUpdate)
	{
		int oldInput = this.clockwiseInput;
		this.clockwiseInput = input;
		if (forceOutputUpdate || oldInput != this.clockwiseInput)
		{
			this.updateOutput();
			this.setChanged();
		}
	}
	
	public void setCounterclockwiseInputOnChannel(boolean hasInput, int channel)
	{
		int newInput = this.counterClockwiseInput;
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setCounterclockwiseInput(newInput, false);
	}
	
	public void setCounterclockwiseInput(int input, boolean forceOutputUpdate)
	{
		int oldInput = this.counterClockwiseInput;
		this.counterClockwiseInput = input;
		if (forceOutputUpdate || oldInput != this.counterClockwiseInput)
		{
			this.updateOutput();
			this.setChanged();
		}
	}
	
	public void updateOutput()
	{
		if (this.getBlockState().getBlock() instanceof TwoInputBitwiseGateBlock block)
		{
			byte output = 0;
			for (int i=0; i<16; i++)
			{
				int bit = 1 << i;
				boolean cwInputBit = (this.clockwiseInput & bit) != 0;
				boolean ccwInputBit = (this.counterClockwiseInput & bit) != 0;
				if (block.operator.apply(cwInputBit, false, ccwInputBit))
				{
					output |= bit;
				}
			}
			this.setOutput(output);
		}
	}

	public Map<Channel, BiConsumer<LevelAccessor, Integer>> getReceiverEndpoints(InputSide side)
	{
		if (side == InputSide.A)
		{
			return this.clockwiseReceiverEndpoints;
		}
		else if (side == InputSide.C)
		{
			return this.counterClockwiseReceiverEndpoints;
		}
		else
			return Map.of();
	}

	@Override
	public void saveAdditional(CompoundTag compound, Provider registries)
	{
		super.saveAdditional(compound, registries);
		compound.putInt(CLOCKWISE_INPUT, this.clockwiseInput);
		compound.putInt(COUNTERCLOCKWISE_INPUT, this.counterClockwiseInput);
	}

	@Override
	public void loadAdditional(CompoundTag compound, Provider registries)
	{
		super.loadAdditional(compound, registries);
		this.clockwiseInput = compound.getInt(CLOCKWISE_INPUT);
		this.counterClockwiseInput = compound.getInt(COUNTERCLOCKWISE_INPUT);
	}
	
}
