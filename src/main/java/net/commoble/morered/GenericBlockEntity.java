package net.commoble.morered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class GenericBlockEntity extends BlockEntity
{
	public static final String DATA = "data";
	public static final String ATTACHMENTS = "attachments";
	
	private final Set<DataComponentType<?>> serverDataComponents;
	private final Set<DataComponentType<?>> syncedDataComponents;
	private final Set<DataComponentType<?>> itemDataComponents;
	private final Set<AttachmentSerializer<?>> syncedAttachments;
	private final Map<DataComponentType<?>, DataTransformer<?>> dataTransformers;
	private final List<AttachmentTransformer<?>> attachmentTransformers;
	private final PreRemoveSideEffects preRemoveSideEffects;
	
	private Map<DataComponentType<?>, TypedDataComponent<?>> data = new HashMap<>();

	public GenericBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
		Set<DataComponentType<?>> serverDataComponents,
		Set<DataComponentType<?>> syncedDataComponents,
		Set<DataComponentType<?>> itemDataComponents,
		Set<AttachmentSerializer<?>> syncedAttachments,
		Map<DataComponentType<?>, DataTransformer<?>> dataTransformers,
		List<AttachmentTransformer<?>> attachmentTransformers,
		PreRemoveSideEffects preRemoveSideEffects)
	{
		super(type,pos,state);
		this.serverDataComponents = serverDataComponents;
		this.syncedDataComponents = syncedDataComponents;
		this.itemDataComponents = itemDataComponents;
		this.syncedAttachments = syncedAttachments;
		this.dataTransformers = dataTransformers;
		this.attachmentTransformers = attachmentTransformers;
		this.preRemoveSideEffects = preRemoveSideEffects;
	}
	
	@SuppressWarnings("unchecked")
	public <T> @Nullable T get(DataComponentType<T> type)
	{
		var holder = this.data.get(type);
		return holder == null ? null : (T)holder.value();
	}
	
	public <T> void set(DataComponentType<T> type, T value)
	{
		if (value == null)
		{
			this.remove(type);
			return;
		}
		var existingHolder = this.data.get(type);
		if (existingHolder == null || !Objects.equals(value, existingHolder.value()))
		{
			// data is changing, mark for updating
			this.data.put(type, TypedDataComponent.createUnchecked(type, value));
			if (!this.level.isClientSide)
			{
				if (this.serverDataComponents.contains(type))
				{
					this.setChanged();
				}
				if (this.syncedDataComponents.contains(type))
				{
					this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
				}
			}
		}
	}
	
	private <T> @Nullable TypedDataComponent<T> getTypedDataForSave(DataComponentType<T> type, BlockState state, RegistryLookup.Provider registries)
	{
		@SuppressWarnings("unchecked")
		TypedDataComponent<T> base = (TypedDataComponent<T>) this.data.get(type);
		if (base == null)
			return null;
		@SuppressWarnings("unchecked")
		@Nullable DataTransformer<T> transformer = (DataTransformer<T>) this.dataTransformers.get(type);
		return transformer == null
			? base
			: transformer.typedTransformOnSave(state, registries, base);
	}
	
	private <T> @Nullable TypedDataComponent<T> getTypedDataForLoad(TypedDataComponent<T> baseData, BlockState state, RegistryLookup.Provider registries)
	{
		DataComponentType<T> type = baseData.type();
		@SuppressWarnings("unchecked")
		@Nullable DataTransformer<T> transformer = (DataTransformer<T>) this.dataTransformers.get(type);
		return transformer == null
			? baseData
			: new TypedDataComponent<>(type, transformer.onLoad.apply(state, registries, baseData.value()));
	}
	
	@SuppressWarnings("unchecked")
	public <T> @Nullable T remove(DataComponentType<T> type)
	{
		var holder = this.data.remove(type);
		var removedValue = holder == null ? null : holder.value();
		if (removedValue != null)
		{
			if (this.serverDataComponents.contains(type))
			{
				this.setChanged();
			}
			if (this.syncedDataComponents.contains(type))
			{
				this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
			}
		}
		return removedValue == null ? null : (T)removedValue;
	}

	@Override
	public void setChanged()
	{
		super.setChanged();
		if (!this.syncedAttachments.isEmpty())
		{
			this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, Provider provider)
	{
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		if (this.attachmentTransformers.isEmpty())
		{
			super.saveAdditional(tag, provider);
		}
		else // override the superbehavior so we can transform attachment data
		{
			var customPersistentData = this.getPersistentData();
			if (!customPersistentData.isEmpty())
			{
				tag.put("NeoForgeData", customPersistentData.copy());
			}
			// attachment internals are super locked down, so we have to serialize everything first
			// then we can reserialize any transformed attachments
			var attachmentsTag = serializeAttachments(provider);
			if (attachmentsTag != null)
			{
				for (var transformer : this.attachmentTransformers)
				{
					transformer.serialize(this, attachmentsTag, provider, ops);
				}

				tag.put(ATTACHMENTS_NBT_KEY, attachmentsTag);
			}
		}
		CompoundTag dataTag = new CompoundTag();
		for (DataComponentType<?> type : this.serverDataComponents)
		{
			Codec<?> codec = type.codec();
			// shouldn't happen since we don't keep transient components
			// but would be nice to check anyway
			if (codec == null)
				continue;
			var serializableValue = this.getTypedDataForSave(type, getBlockState(), provider);
			if (serializableValue != null && serializableValue.value() != null)
			{
				serializableValue.encodeValue(ops)
					.result()
					.ifPresent(valueTag -> dataTag.put(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(serializableValue.type()).toString(), valueTag));
			}
		}
		tag.put(DATA, dataTag);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, Provider provider)
	{
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		CompoundTag attachmentsTag = tag.getCompoundOrEmpty(ATTACHMENTS_NBT_KEY);
		for (var transformer : this.attachmentTransformers)
		{
			// attachment internals are locked down super hard and I don't feel like adding more accessor mixins
			// so, for each transformable attachment we have,
			// we have to read it, transform it, write it back to the tag
			// then let super.loadAdditional run over the tag again
			transformer.denormalize(getBlockState(), attachmentsTag, provider, ops);
		}
		tag.put(ATTACHMENTS_NBT_KEY, attachmentsTag);
		super.loadAdditional(tag, provider);
		CompoundTag dataTag = tag.getCompoundOrEmpty(DATA);
		for (DataComponentType<?> type : this.serverDataComponents)
		{
			Codec<?> codec = type.codec();
			// shouldn't happen since we don't keep transient components
			// but would be nice to check anyway
			if (codec == null)
				continue;
			String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
			@Nullable Tag valueTag = dataTag.get(key);
			if (valueTag != null)
			{
				codec.parse(ops,valueTag)
					.result()
					.ifPresent(value -> this.data.put(type, this.getTypedDataForLoad(TypedDataComponent.createUnchecked(type,value), this.getBlockState(), provider)));
			}
		}
	}

	@Override
	public CompoundTag getUpdateTag(Provider provider)
	{
		var tag = super.getUpdateTag(provider);	// empty tag
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		
		CompoundTag dataTag = new CompoundTag();
		for (DataComponentType<?> type : this.syncedDataComponents)
		{
			var holder = this.data.get(type);
			if (holder != null && holder.value() != null)
			{
				holder.encodeValue(ops)
					.result()
					.ifPresent(valueTag -> dataTag.put(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(holder.type()).toString(), valueTag));
			}
		}
		tag.put(DATA, dataTag);
		
		CompoundTag attachmentsTag = new CompoundTag();
		for (AttachmentSerializer<?> serializer : this.syncedAttachments)
		{
			serializer.write(attachmentsTag, this, ops);
		}
		tag.put(ATTACHMENTS, attachmentsTag);
		
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, Provider provider)
	{
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		
		CompoundTag dataTag = tag.getCompoundOrEmpty(DATA);
		Map<DataComponentType<?>, TypedDataComponent<?>> data = new HashMap<>();
		for (DataComponentType<?> type : this.syncedDataComponents)
		{
			String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
			if (dataTag.contains(key))
			{
				Tag valueTag = dataTag.get(key);
				type.codec().parse(ops,valueTag)
					.result()
					.ifPresent(value -> data.put(type, TypedDataComponent.createUnchecked(type, value)));
			}
		}
		this.data = data;
		
		CompoundTag attachmentTag = tag.getCompoundOrEmpty(ATTACHMENTS);
		for (var serializer : this.syncedAttachments)
		{
			serializer.read(attachmentTag, this, ops);
		}
		
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, Provider lookupProvider)
	{
		this.handleUpdateTag(pkt.getTag(), lookupProvider);
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter input)
	{
		super.applyImplicitComponents(input);
		for (var type : this.itemDataComponents)
		{
			var value = input.get(type);
			this.data.put(type, TypedDataComponent.createUnchecked(type, value));
		}
	}

	@Override
	protected void collectImplicitComponents(Builder builder)
	{
		super.collectImplicitComponents(builder);
		for (var type : this.itemDataComponents)
		{
			var holder = this.data.get(type);
			setImplicitComponent(builder, holder);
		}
	}
	
	private static <T> void setImplicitComponent(Builder builder, TypedDataComponent<T> holder)
	{
		builder.set(holder.type(), holder.value());
	}

	@Override
	@Deprecated
	public void removeComponentsFromTag(CompoundTag tag)
	{
		super.removeComponentsFromTag(tag);
		if (tag.contains(DATA))
		{
			CompoundTag dataTag = tag.getCompoundOrEmpty(DATA);
			for (var type : this.itemDataComponents)
			{
				String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
				dataTag.remove(key);
			}
		}
	}
	
	
	
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state)
	{
		this.preRemoveSideEffects.apply(pos, state, this);
	}

	public static GenericBlockEntityBuilder builder()
	{
		return new GenericBlockEntityBuilder(
			new HashSet<>(),
			new HashSet<>(),
			new HashSet<>(),
			new HashSet<>(),
			new HashMap<>(),
			new ArrayList<>(),
			new MutableObject<>(PreRemoveSideEffects.NONE)
		);
	}
	
	public static record GenericBlockEntityBuilder(
		Set<Supplier<? extends DataComponentType<?>>> serverDataComponents,
		Set<Supplier<? extends DataComponentType<?>>> syncedDataComponents,
		Set<Supplier<? extends DataComponentType<?>>> itemDataComponents,
		Set<AttachmentSerializer<?>> syncedAttachments,
		Map<Supplier<? extends DataComponentType<?>>, DataTransformer<?>> dataTransformers,
		List<AttachmentTransformer<?>> attachmentTransformers,
		MutableObject<PreRemoveSideEffects> preRemoveSideEffects
	)
	{
		/**
		 * Specifies a server (unsynced) data component
		 * @param types DataComponentTypes to save but not sync
		 * @return this
		 */
		@SafeVarargs
		public final GenericBlockEntityBuilder serverData(Supplier<? extends DataComponentType<?>>...types)
		{
			for (var type : types)
			{
				this.serverDataComponents.add(type);
			}
			return this;
		}
		
		/**
		 * Specifies a synced datacomponent. Non-transient types are also automatically marked as server data components.
		 * @param types DataComponentTypes to sync and save. Types with null codecs are ignored (transient or not)
		 * @return this
		 */
		@SafeVarargs
		public final GenericBlockEntityBuilder syncedData(Supplier<? extends DataComponentType<?>>...types)
		{
			for (var type : types)
			{
				this.syncedDataComponents.add(type);
			}
			return this.serverData(types);
		}
		
		/**
		 * Specifies an item data component. These components are automatically pulled from blockitem data on place,
		 * and are provided to loot table context on destruction.
		 * These are always synced and saved as well, and are automatically marked as synced+server datacomponents
		 * (except for transient components which are not marked as server components, and types without codecs, which cannot be used as server/sync components)
		 * @param types DataComponentTypes to sync, save, and associate with blockitem components
		 * @return this
		 */
		@SafeVarargs
		public final GenericBlockEntityBuilder itemData(Supplier<? extends DataComponentType<?>>... types)
		{
			for (var type : types)
			{
				this.itemDataComponents.add(type);
			}
			return this.syncedData(types);
		}
		
		/**
		 * Specifies a synced data attachment. Any specified data attachments present on the blockentity when synced will be sent to the client.
		 * @param <T> data type
		 * @param type AttachmentType
		 * @param codec Codec to use for serializing (because norge doesn't make the type's serializer public for whatever reason)
		 * @return this
		 */
		public <T> GenericBlockEntityBuilder syncAttachment(Supplier<AttachmentType<T>> type, Codec<T> codec)
		{
			this.syncedAttachments.add(new AttachmentSerializer<>(type,codec));
			return this;
		}
		
		/**
		 * Registers a data transformer used for altering data on save/load.
		 * Used for e.g. rotating/mirroring data in structures.
		 * Only applies on serverside save load, does not affect synced data
		 * @param <T>
		 * @param type DataComponentType
		 * @param onSave Function3 to apply to data on save
		 * @param onLoad Function3 to apply to data on load
		 * @return this
		 */
		public <T> GenericBlockEntityBuilder dataTransformer(
			Supplier<DataComponentType<T>> type,
			Function3<BlockState,HolderLookup.Provider,T,T> onSave,
			Function3<BlockState,HolderLookup.Provider,T,T> onLoad)
		{
			this.dataTransformers.put(type, new DataTransformer<>(onSave, onLoad));
			return this;
		}
		
		public <T> GenericBlockEntityBuilder transformAttachment(
			Supplier<AttachmentType<T>> type,
			Codec<T> codec,
			Function3<BlockState,HolderLookup.Provider,T,T> onSave,
			Function3<BlockState,HolderLookup.Provider,T,T> onLoad)
		{
			this.attachmentTransformers.add(new AttachmentTransformer<>(new AttachmentSerializer<>(type,codec),onSave,onLoad));
			return this;
		}
		
		public GenericBlockEntityBuilder preRemoveSideEffects(PreRemoveSideEffects effects)
		{
			this.preRemoveSideEffects.setValue(effects);
			return this;
		}

		@SuppressWarnings("unchecked")
		public DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> register(
			DeferredRegister<BlockEntityType<?>> blockEntities,
			String name,
			Collection<? extends Supplier<? extends Block>> blocks)
		{
			return this.register(blockEntities, name, blocks.stream().toArray(Supplier[]::new));
		}

		@SafeVarargs
		public final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> register(
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
					// datacomponenttypes register before blockentitytypes, hopefully
						(pos,state) -> new GenericBlockEntity(holder.get(), pos, state,
							serverDataComponents.stream().map(Supplier::get).filter(type -> !type.isTransient() && type.codec() != null).collect(Collectors.toSet()),
							syncedDataComponents.stream().map(Supplier::get).filter(type -> type.codec() != null).collect(Collectors.toSet()),
							itemDataComponents.stream().map(Supplier::get).collect(Collectors.toSet()),
							syncedAttachments,
							dataTransformers.entrySet().stream().collect(Collectors.toMap(
								entry -> entry.getKey().get(),
								Map.Entry::getValue)),
							attachmentTransformers,
							this.preRemoveSideEffects.getValue()),
						Arrays.stream(blocks).map(Supplier::get).toArray(Block[]::new))
				);
			return holder;
		}
	}
		
	private static record AttachmentSerializer<T>(Supplier<AttachmentType<T>> type, Codec<T> codec)
	{
		public void write(CompoundTag tag, IAttachmentHolder holder, RegistryOps<Tag> ops)
		{
			AttachmentType<T> theType = this.type.get();
			@Nullable T data = holder.getData(theType);
			write(tag,data,ops);
		}
		
		public void write(CompoundTag tag, T data, RegistryOps<Tag> ops)
		{
			AttachmentType<T> theType = this.type.get();
			String key = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(theType).toString();
			if (data != null)
			{
				this.codec.encodeStart(ops, data)
					.result()
					.ifPresent(dataTag -> tag.put(key, dataTag));
			}
		}
		
		public void read(CompoundTag tag, IAttachmentHolder holder, RegistryOps<Tag> ops)
		{
			AttachmentType<T> theType = this.type.get();
			String key = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(theType).toString();
			if (tag.contains(key))
			{
				Tag dataTag = tag.get(key);
				this.codec.parse(ops, dataTag)
					.resultOrPartial(e -> holder.removeData(type))
					.ifPresent(data -> holder.setData(theType, data));
			}
			else
			{
				holder.removeData(theType);
			}
		}
		
		public Optional<T> read(CompoundTag attachmentsTag, RegistryOps<Tag> ops)
		{
			AttachmentType<T> theType = this.type.get();
			String key = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(theType).toString();
			if (attachmentsTag.contains(key))
			{
				Tag dataTag = attachmentsTag.get(key);
				return this.codec.parse(ops, dataTag).result();
			}
			else
			{
				return Optional.empty();
			}
		}
	}
	
	private static record DataTransformer<T>(
		Function3<BlockState,HolderLookup.Provider,T,T> onSave,
		Function3<BlockState,HolderLookup.Provider,T,T> onLoad)
	{
		public TypedDataComponent<T> typedTransformOnSave(BlockState state, HolderLookup.Provider registries, TypedDataComponent<T> typedData)
		{
			return new TypedDataComponent<>(typedData.type(), this.onSave.apply(state, registries, typedData.value()));
		}
	}
	
	private static record AttachmentTransformer<T>(
		AttachmentSerializer<T> serializer,
		Function3<BlockState,HolderLookup.Provider,T,T> onSave,
		Function3<BlockState,HolderLookup.Provider,T,T> onLoad)
	{
		public void serialize(GenericBlockEntity be, CompoundTag attachmentsTag, HolderLookup.Provider provider, RegistryOps<Tag> ops)
		{
			T denormalizedData = be.getData(this.serializer.type.get());
			T normalizedData = this.onSave.apply(be.getBlockState(), provider, denormalizedData);
			this.serializer.write(attachmentsTag, normalizedData, ops);
		}
		
		public void denormalize(BlockState state, CompoundTag attachmentsTag, HolderLookup.Provider provider, RegistryOps<Tag> ops)
		{
			this.serializer.read(attachmentsTag, ops).ifPresent(normalizedData -> {
				T denormalizedData = this.onLoad.apply(state, provider, normalizedData);
				this.serializer.write(attachmentsTag, denormalizedData, ops);
			});
		}
	}
	
	@FunctionalInterface
	public static interface PreRemoveSideEffects
	{
		public static final PreRemoveSideEffects NONE = (pos,newState,be) -> {};
		public abstract void apply(BlockPos pos, BlockState newState, GenericBlockEntity be);
	}
}
