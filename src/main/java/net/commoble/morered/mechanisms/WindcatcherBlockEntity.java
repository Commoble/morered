package net.commoble.morered.mechanisms;

import java.util.Objects;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.morered.MoreRed;
import net.commoble.morered.Names;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WindcatcherBlockEntity extends BlockEntity
{
	public static final String MECHANICAL_STATE = "mechanical_state";
	
	private WindcatcherColors colors = WindcatcherColors.DEFAULT;

	public WindcatcherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public WindcatcherBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().windcatcherBlockEntity.get(), pos, state);
	}

	@Override
	public void setChanged()
	{
		super.setChanged();
		// send to client the mechanical state attached by the generic mechanical component
		this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, Provider provider)
	{
		super.saveAdditional(tag, provider);
		tag.put(Names.WINDCATCHER_COLORS, this.colors.toTag());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, Provider provider)
	{
		super.loadAdditional(tag, provider);
		this.colors = WindcatcherColors.fromTag(tag.getCompound(Names.WINDCATCHER_COLORS));
	}

	@Override
	public CompoundTag getUpdateTag(Provider provider)
	{
		var tag = super.getUpdateTag(provider);	// empty tag
		tag.put(Names.WINDCATCHER_COLORS, this.colors.toTag());
		var data = this.getData(MechanicalNodeStates.HOLDER.get());
		// mechanical state is saved as a data attachment by the generic mechanical component
		// but we can't make that automatically sync... so we have to do it ourself, but only in sync methods, not save/load
		// TODO replace that with datacomponents when blockentities have better support for them
		MechanicalNodeStates.CODEC.encodeStart(NbtOps.INSTANCE, data)
			.result()
			.ifPresent(dataTag -> tag.put(MECHANICAL_STATE, dataTag));
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, Provider lookupProvider)
	{
		super.handleUpdateTag(tag, lookupProvider); //loadAdditional
		var dataTag = tag.get(MECHANICAL_STATE);
		if (dataTag != null)
		{
			MechanicalNodeStates.CODEC.parse(NbtOps.INSTANCE, dataTag)
				.result()
				.ifPresent(data -> this.setData(MechanicalNodeStates.HOLDER.get(), data));
		}
		
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, Provider lookupProvider)
	{
		this.handleUpdateTag(pkt.getTag(), lookupProvider);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input)
	{
		super.applyImplicitComponents(input);
		this.colors = Objects.requireNonNullElse(input.get(MoreRed.get().windcatcherColorsDataComponent.get()), WindcatcherColors.DEFAULT);
	}

	@Override
	protected void collectImplicitComponents(Builder builder)
	{
		super.collectImplicitComponents(builder);
		builder.set(MoreRed.get().windcatcherColorsDataComponent.get(), this.colors);
	}

	@Override
	@Deprecated
	public void removeComponentsFromTag(CompoundTag tag)
	{
		super.removeComponentsFromTag(tag);
		tag.remove(Names.WINDCATCHER_COLORS);
	}
	
	
}
