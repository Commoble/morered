package net.commoble.exmachina.internal.testcontent;

import java.util.Arrays;
import java.util.function.Supplier;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GenericBlockEntity extends BlockEntity
{
	public GenericBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	@SafeVarargs
	public static DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> register(
		DeferredRegister<BlockEntityType<?>> blockEntities,
		String name,
		Supplier<? extends Block>... blocks)
	{
		DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> holder = DeferredHolder.create(
			ResourceKey.create(
				Registries.BLOCK_ENTITY_TYPE,
				ResourceLocation.fromNamespaceAndPath(
					blockEntities.getNamespace(),
					name)));
		blockEntities.register(
			name,
			() -> new BlockEntityType<>(
				(pos,state) -> new GenericBlockEntity(holder.get(), pos, state),
				Arrays.stream(blocks).map(Supplier::get).toArray(Block[]::new)));
		return holder;
	}

	@Override
	public void setChanged()
	{
		super.setChanged();
		this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(Provider provider)
	{
		var tag = super.getUpdateTag(provider);
		var data = this.getData(MechanicalNodeStates.HOLDER.get());
		MechanicalNodeStates.CODEC.encodeStart(NbtOps.INSTANCE, data)
			.result()
			.ifPresent(dataTag -> tag.put("mechanical_state", dataTag));
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, Provider lookupProvider)
	{
		super.handleUpdateTag(tag, lookupProvider);
		var dataTag = tag.get("mechanical_state");
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
		super.onDataPacket(net, pkt, lookupProvider);
		var dataTag = pkt.getTag().get("mechanical_state");
		if (dataTag != null)
		{
			MechanicalNodeStates.CODEC.parse(NbtOps.INSTANCE, dataTag)
				.result()
				.ifPresent(data -> this.setData(MechanicalNodeStates.HOLDER.get(), data));
		}
	}
	
	
}
