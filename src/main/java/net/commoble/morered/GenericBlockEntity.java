package net.commoble.morered;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.commoble.morered.util.DelegatingValueInput;
import net.commoble.morered.util.DelegatingValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GenericBlockEntity extends BlockEntity
{
	public static final String ATTACHMENTS = "attachments";
	
	private final Set<DataComponentType<?>> serverDataComponents;
	private final Set<DataComponentType<?>> syncedDataComponents;
	private final Set<DataComponentType<?>> itemDataComponents;
	private final Map<DataComponentType<?>, DataTransformer<?>> dataTransformers;
	private final Map<MapCodec<?>, AttachmentTransformer<?>> attachmentTransformers;
	private final PreRemoveSideEffects preRemoveSideEffects;
	
	private Map<DataComponentType<?>, TypedDataComponent<?>> data = new HashMap<>();

	public GenericBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
		Set<DataComponentType<?>> serverDataComponents,
		Set<DataComponentType<?>> syncedDataComponents,
		Set<DataComponentType<?>> itemDataComponents,
		Map<DataComponentType<?>, DataTransformer<?>> dataTransformers,
		Map<MapCodec<?>, AttachmentTransformer<?>> attachmentTransformers,
		PreRemoveSideEffects preRemoveSideEffects)
	{
		super(type,pos,state);
		this.serverDataComponents = serverDataComponents;
		this.syncedDataComponents = syncedDataComponents;
		this.itemDataComponents = itemDataComponents;
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
	
	public <T> T getOrDefault(DataComponentType<T> type, T defaultValue)
	{
		T value = this.get(type);
		return value == null ? defaultValue : value;
	}
	
	public <T> T getOrCreate(DataComponentType<T> type, T defaultValue)
	{
		T value = this.get(type);
		if (value == null)
		{
			this.set(type, defaultValue);
			value = defaultValue;
		}
		return value;
	}
	
	public <T> T getOrCreate(DataComponentType<T> type, Supplier<T> defaultValueSupplier)
	{
		T value = this.get(type);
		if (value == null)
		{
			value = defaultValueSupplier.get();
			this.set(type, value);
		}
		return value;
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
	
	private <T> @Nullable TypedDataComponent<T> getTypedDataForSave(DataComponentType<T> type, BlockState state)
	{
		@SuppressWarnings("unchecked")
		TypedDataComponent<T> base = (TypedDataComponent<T>) this.data.get(type);
		if (base == null)
			return null;
		@SuppressWarnings("unchecked")
		@Nullable DataTransformer<T> transformer = (DataTransformer<T>) this.dataTransformers.get(type);
		return transformer == null
			? base
			: transformer.typedTransformOnSave(state, base);
	}
	
	private <T> @Nullable TypedDataComponent<T> getTypedDataForLoad(TypedDataComponent<T> baseData, BlockState state)
	{
		DataComponentType<T> type = baseData.type();
		@SuppressWarnings("unchecked")
		@Nullable DataTransformer<T> transformer = (DataTransformer<T>) this.dataTransformers.get(type);
		return transformer == null
			? baseData
			: new TypedDataComponent<>(type, transformer.onLoad.apply(state, baseData.value()));
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
	}

	@Override
	protected void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(new TransformingValueOutput(output, this)); // use delegator to finagle transformable attachments
		for (DataComponentType<?> type : this.serverDataComponents)
		{
			Codec<?> codec = type.codec();
			// shouldn't happen since we don't keep transient components
			// but would be nice to check anyway
			if (codec == null)
				continue;
			var serializableValue = this.getTypedDataForSave(type, getBlockState());
			if (serializableValue != null && serializableValue.value() != null)
			{
				storeDataComponent(output, serializableValue);
			}
		}
	}
	
	private static <T> void storeDataComponent(ValueOutput dataTag, TypedDataComponent<T> typedData)
	{
		DataComponentType<T> type = typedData.type();
		String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
		dataTag.store(key, type.codec(), typedData.value());
	}

	@Override
	protected void loadAdditional(ValueInput input)
	{
		super.loadAdditional(new TransformingValueInput(input, this)); // use delegator to finagle transformable attachments
		for (DataComponentType<?> type : this.serverDataComponents)
		{
			Codec<?> codec = type.codec();
			// shouldn't happen since we don't keep transient components
			// but would be nice to check anyway
			if (codec == null)
				continue;
			String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
			input.read(key, codec)
				.ifPresent(value -> this.data.put(type, this.getTypedDataForLoad(TypedDataComponent.createUnchecked(type,value), this.getBlockState())));
		}
	}

	@Override
	public CompoundTag getUpdateTag(Provider provider)
	{
		var tag = super.getUpdateTag(provider);	// empty tag
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		
		for (DataComponentType<?> type : this.syncedDataComponents)
		{
			var holder = this.data.get(type);
			if (holder != null && holder.value() != null)
			{
				holder.encodeValue(ops)
					.result()
					.ifPresent(valueTag -> tag.put(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(holder.type()).toString(), valueTag));
			}
		}
		
		return tag;
	}

	@Override
	public void handleUpdateTag(ValueInput input)
	{
		Map<DataComponentType<?>, TypedDataComponent<?>> data = new HashMap<>();
		for (DataComponentType<?> type : this.syncedDataComponents)
		{
			String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
			input.read(key, type.codec())
				.ifPresent(value -> data.put(type, TypedDataComponent.createUnchecked(type, value)));
		}
		this.data = data;		
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ValueInput input)
	{
		this.handleUpdateTag(input);
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
			if (holder != null) {
				setImplicitComponent(builder, holder);
			}
		}
	}
	
	private static <T> void setImplicitComponent(Builder builder, TypedDataComponent<T> holder)
	{
		builder.set(holder.type(), holder.value());
	}

	@Override
	@Deprecated
	public void removeComponentsFromTag(ValueOutput output)
	{
		super.removeComponentsFromTag(output);
		for (var type : this.itemDataComponents)
		{
			String key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
			output.discard(key);
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
			new HashMap<>(),
			new Reference2ObjectOpenHashMap<>(),
			new MutableObject<>(PreRemoveSideEffects.NONE)
		);
	}
	
	public static record GenericBlockEntityBuilder(
		Set<Supplier<? extends DataComponentType<?>>> serverDataComponents,
		Set<Supplier<? extends DataComponentType<?>>> syncedDataComponents,
		Set<Supplier<? extends DataComponentType<?>>> itemDataComponents,
		Map<Supplier<? extends DataComponentType<?>>, DataTransformer<?>> dataTransformers,
		Map<MapCodec<?>, AttachmentTransformer<?>> attachmentTransformers,
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
			BiFunction<BlockState,T,T> onSave,
			BiFunction<BlockState,T,T> onLoad)
		{
			this.dataTransformers.put(type, new DataTransformer<>(onSave, onLoad));
			return this;
		}
		
		public <T> GenericBlockEntityBuilder transformAttachment(
			Supplier<AttachmentType<T>> type,
			MapCodec<T> codec,
			BiFunction<BlockState,T,T> onSave,
			BiFunction<BlockState,T,T> onLoad)
		{
			this.attachmentTransformers.put(codec, new AttachmentTransformer<>(onSave,onLoad));
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
	
	private static record DataTransformer<T>(
		BiFunction<BlockState,T,T> onSave,
		BiFunction<BlockState,T,T> onLoad)
	{
		public TypedDataComponent<T> typedTransformOnSave(BlockState state, TypedDataComponent<T> typedData)
		{
			return new TypedDataComponent<>(typedData.type(), this.onSave.apply(state, typedData.value()));
		}
	}
	
	private static record AttachmentTransformer<T>(
		BiFunction<BlockState,T,T> onSave,
		BiFunction<BlockState,T,T> onLoad)
	{
		public T normalize(BlockEntity be, T denormalizedData)
		{
			return this.onSave.apply(be.getBlockState(), denormalizedData);
		}
		
		public T denormalize(BlockEntity be, T normalizedData)
		{
			return this.onLoad.apply(be.getBlockState(), normalizedData);
		}
	}
	
	@FunctionalInterface
	public static interface PreRemoveSideEffects
	{
		public static final PreRemoveSideEffects NONE = (pos,newState,be) -> {};
		public abstract void apply(BlockPos pos, BlockState newState, GenericBlockEntity be);
	}
	
	private static record TransformingValueOutput(ValueOutput delegate, GenericBlockEntity be) implements DelegatingValueOutput
	{
		@Deprecated
		@Override
		public <T> void store(MapCodec<T> codec, T value)
		{
			var transformer = be.attachmentTransformers.get(codec);
			if (transformer == null) // don't transform data
			{
				delegate.store(codec, value);
			}
			else // transform data
			{
				@SuppressWarnings("unchecked")
				AttachmentTransformer<T> castTransformer = (AttachmentTransformer<T>)transformer;
				T normalizedValue = castTransformer.normalize(be, value);
				delegate.store(codec, normalizedValue);
			}
		}
	}
	
	private static record TransformingValueInput(ValueInput delegate, GenericBlockEntity be) implements DelegatingValueInput
	{
		@Override
		@Deprecated
		public <T> Optional<T> read(MapCodec<T> codec)
		{
			var transformer = be.attachmentTransformers.get(codec);
			Optional<T> readValue = delegate.read(codec);
			if (transformer == null) // don't transform data
			{
				return readValue;
			}
			else // transform data
			{
				@SuppressWarnings("unchecked")
				AttachmentTransformer<T> castTransformer = (AttachmentTransformer<T>)transformer;
				return readValue.map(normalizedValue -> castTransformer.denormalize(be, normalizedValue));
			}
		}
	}
}
