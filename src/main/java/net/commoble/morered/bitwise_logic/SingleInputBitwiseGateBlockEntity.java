package net.commoble.morered.bitwise_logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.Receiver;
import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.InputSide;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SingleInputBitwiseGateBlockEntity extends BitwiseGateBlockEntity
{
	public static final String INPUT = "input";
	
	protected int input = 0;
	protected Map<Channel, Receiver> receiverEndpoints = Util.make(() -> {
		Map<Channel, Receiver> map = new HashMap<>();
		for (DyeColor color : DyeColor.values())
		{
			final int channel = color.ordinal(); 
			map.put(Channel.single(color), new BitwiseListener(color, InputSide.B, (levelAccess, power) -> this.setInputOnChannel(power > 0, channel)));
		}
		
		return map;
	});
	protected Map<Channel, Collection<Receiver>> allReceivers = Util.make(() -> {
		Map<Channel, Collection<Receiver>> map = new HashMap<>();
		this.receiverEndpoints.forEach((channel, receiver) -> {
			map.put(channel, List.of(receiver));	
		});
		
		return map;
	});

	public SingleInputBitwiseGateBlockEntity(BlockEntityType<? extends SingleInputBitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public static SingleInputBitwiseGateBlockEntity create(BlockPos pos, BlockState state)
	{
		return new SingleInputBitwiseGateBlockEntity(MoreRed.get().singleInputBitwiseGateBeType.get(), pos, state);
	}
	
	public void setInputOnChannel(boolean hasInput, int channel)
	{
		int newInput = this.input;
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setInput(newInput, false);
	}

	public void setInput(int input, boolean forceOutputUpdate)
	{
		int oldInput = this.input;
		this.input = input;
		if ((forceOutputUpdate || oldInput != this.input) && this.getBlockState().getBlock() instanceof SingleInputBitwiseGateBlock block)
		{
			byte output = 0;
			for (int i=0; i<16; i++)
			{
				int bit = 1 << i;
				boolean inputBit = (input & bit) != 0;
				if (block.operator.apply(false, inputBit, false))
				{
					output |= bit;
				}
			}
			this.setOutput(output);
			this.setChanged();
		}
	}

	@Override
	public void saveAdditional(CompoundTag compound, Provider registries)
	{
		super.saveAdditional(compound, registries);
		compound.putInt(INPUT, this.input);
	}

	@Override
	public void loadAdditional(CompoundTag compound, Provider registries)
	{
		super.loadAdditional(compound, registries);
		this.input = compound.getInt(INPUT);
	}
	
	public @Nullable Receiver getReceiverEndpoint(Channel channel)
	{
		return this.receiverEndpoints.get(channel);
	}

	public Collection<Receiver> getAllReceivers(Channel channel)
	{
		return this.allReceivers.getOrDefault(channel, List.of());
	}

	@Override
	public void resetUnusedReceivers(Collection<BitwiseListener> receivers)
	{
		int oldInput = this.input;
		for (BitwiseListener receiver : receivers)
		{
			int color = receiver.color().ordinal();
			int bit = 1 << color;
			oldInput &= ~bit;
		}
		this.setInput(oldInput, true);
	}
	
}
