/*

The MIT License (MIT)

Copyright (c) 2022 Joseph Bettendorff a.k.a. "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */
package commoble.morered.datagen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Alternative datageneration for blockstate jsons. Usable with {@link JsonCodecProvider}.
 */
public record BlockStateFile(Optional<Variants> variants, Optional<Multipart> multipart)
{
	/** codec **/
	public static final Codec<BlockStateFile> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Variants.CODEC.optionalFieldOf("variants").forGetter(BlockStateFile::variants),
			Multipart.CODEC.optionalFieldOf("multipart").forGetter(BlockStateFile::multipart)
		).apply(builder, BlockStateFile::new));
	
	/**
	 * Creates a DataProvider and adds it to the provided GatherDataEvent's datagenerator, generating in the assets/namespace/blockstates/ folder
	 * @param event GatherDataEvent with datagen context
	 * @param modid String modid for logging purposes
	 * @param dynamicOps DynamicOps to serialize data to json with, e.g. JsonOps.INSTANCE
	 * @param entries Map of ResourceLocation ids to BlockStateDefinitions to serialize
	 */
	public static void addDataProvider(GatherDataEvent event, String modid, DynamicOps<JsonElement> dynamicOps, Map<ResourceLocation, BlockStateFile> entries)
	{
		DataGenerator dataGenerator = event.getGenerator();
		dataGenerator.addProvider(event.includeClient(), new JsonCodecProvider<BlockStateFile>(dataGenerator, event.getExistingFileHelper(), modid, dynamicOps, PackType.CLIENT_RESOURCES, "blockstates", CODEC, entries));
	}
	
	/**
	 * Specifies a blockstate file with a variants definition. See {@link Variants#builder}.
	 * @param variants Variants mapping sets of blockstates to individual models.
	 * @return BlockStateDefinition for datageneration.
	 */
	public static BlockStateFile variants(Variants variants)
	{
		return new BlockStateFile(Optional.of(variants), Optional.empty());
	}
	
	/**
	 * Specifies a blockstate file with a multipart definition. See {@link Multipart#builder}.
	 * @param multipart Multipart definition assigning (possibly multiple) models to states.
	 * @return BlockStateDefinition for datageneration.
	 */
	public static BlockStateFile multipart(Multipart multipart)
	{
		return new BlockStateFile(Optional.empty(), Optional.of(multipart));
	}
	
	/**
	 * Specifies a blockstate file with both variants and multipart definitions. Variants override multiparts.
	 * @param variants Variants mapping sets of blockstates to individual models.
	 * @param multipart Multipart definition assigning (possibly multiple) models to states.
	 * @return BlockStateDefinition for datageneration
	 */
	public static BlockStateFile variantsAndMultipart(Variants variants, Multipart multipart)
	{
		return new BlockStateFile(Optional.of(variants), Optional.of(multipart));
	}

	/**
	 * Represents a "variants" block in a blockstate json.
	 * @param variants Map of blockstate property predicates to models
	 */
	public static record Variants(Map<List<PropertyValue<?>>, List<Model>> variants)
	{
		/** codec **/
		public static final Codec<Variants> CODEC = Codec.unboundedMap(
				PropertyValue.LIST_CODEC,
				new ExtraCodecs.EitherCodec<>(Model.CODEC, Model.CODEC.listOf()).xmap(
					either -> either.map(List::of, Function.identity()),
					list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)))
			.xmap(Variants::new, Variants::variants);
		
		/**
		 * Creates and returns a mutable VariantBlockStateDefinition for whom variants can be defined.
		 * Once variants are added to the definition, giving this to a JsonDataProvider
		 * is sufficient to cause the blockstate jsons to be generated, so long as that data provider
		 * is given to the data generator, or if that data provider's act method is called by another acting
		 * data provider's act method.
		 * @return a mutable VariantBlockStateDefinition
		 */
		public static Variants builder()
		{
			return new Variants(new HashMap<>());
		}
		
		/**
		 * Convenience method for a variants blockstate file without blockstate predication
		 * @param models Model definitions to use. More than one indicates a list of random models.
		 * @return new Variants
		 */
		public static Variants always(Model... models)
		{
			return Variants.builder().addVariant(List.of(), models);
		}

		/**
		 * Adds a variant for your blockstate json
		 * @param value e.g. "facing=east"
		 * @param models Model definition, e.g. { "model": "minecraft:block/acacia_stairs_inner" }. Providing more than one
		 * indicates a list of random models (e.g. stone's models).
		 * @return this
		 */
		public Variants addVariant(PropertyValue<?> value, Model... models)
		{
			this.addVariant(List.of(value), models);
			return this;
		}
		
		/**
		 * Adds a variant for your blockstate json
		 * @param values e.g. "facing=east,powered=false"
		 * @param models Model definition, e.g. { "model": "minecraft:block/acacia_stairs_inner" }. Providing more than one
		 * indicates a list of random models (e.g. stone's models).
		 * @return this
		 */
		public Variants addVariant(List<PropertyValue<?>> values, Model... models)
		{
			this.variants.put(values, Arrays.asList(models));
			return this;
		}
	}
	
	/**
	 * Component of Variants definitions, representing a property-value entry.
	 * 
	 * @param property Property of a blockstate.
	 * @param value value of a blockstate Property.
	 */
	public static record PropertyValue<T extends Comparable<T>>(Property<T> property, T value)
	{
		/** codec **/
		public static final Codec<PropertyValue<?>> CODEC = Codec.STRING.comapFlatMap(
			s -> DataResult.error("PropertyValue not deserializable"),
			PropertyValue::toString);
		
		/** list codec **/
		public static final Codec<List<PropertyValue<?>>> LIST_CODEC = Codec.STRING.comapFlatMap(
			s -> DataResult.error("PropertyValue List not deserializable"),
			values -> String.join(",", values.stream().map(PropertyValue::toString).toArray(String[]::new)));
		
		/**
		 * Creates a property value from a property and a value
		 * @param <T> value type
		 * @param property Property of a blockstate
		 * @param value Value of a blockstate property
		 * @return PropertyValue with that property and value
		 */
		public static <T extends Comparable<T>> PropertyValue<T> create(Property<T> property, T value)
		{
			return new PropertyValue<>(property, value);
		}
		
		@Override
		public String toString()
		{
			return this.property.getName() + "=" + this.property.getName(value);
		}
	}
	
	/**
	 * Multipart format for blockstate jsons. Assigns models to blockstates by comparing states to property-value
	 * predicate Cases. Multiple models can be applied to individual blockstates. When used with the Variants format,
	 * models applied by Variants will override models applied by Multiparts.
	 * 
	 * @param cases List of when-apply components.
	 */
	public static record Multipart(List<WhenApply> cases)
	{
		/** codec **/
		public static final Codec<Multipart> CODEC = WhenApply.CODEC.listOf().xmap(Multipart::new, Multipart::cases);

		/**
		 * Creates and returns a MultipartBlockStateDefinition for whom cases can be defined.
		 * Once cases are added to the definition, giving this to a JsonDataProvider
		 * is sufficient to cause the blockstate jsons to be generated, so long as that data provider
		 * is given to the data generator, or if that data provider's act method is called by another acting
		 * data provider's act method.
		 * @return a mutable MultipartBlockStateDefinition
		 */
		public static Multipart builder()
		{
			return new Multipart(new ArrayList<>());
		}
		
		/**
		 * Adds a when-apply case to this multipart definition
		 * @param whenApply WhenApply to add
		 * @return this
		 */
		public Multipart addWhenApply(WhenApply whenApply)
		{
			this.cases.add(whenApply);
			return this;
		}
	}
	
	/**
	 * Component of Multipart definitions, representing the "when" and "apply" blocks
	 * in a blockstate json.
	 * 
	 * @param when Optional field specifying one or more case predicates to apply to each blockstate.
	 * If a Case is provided, the model list will be used for blockstates that match that case.
	 * If an OrCase is provided, the model list will be used for blockstates that match any of the sub-cases.
	 * If no case is provided, the model list will be used for all blockstates. 
	 * @param apply List of models to randomly apply to a blockstate at a position if the 'when' case
	 * applies to that blockstate. Must contain at least one model. If the list contains more than
	 * one model, one model will randomly be applied at each position containing the relevant blockstates.
	 */
	public static record WhenApply(Optional<Either<OrCase, Case>> when, List<Model> apply)
	{
		/** codec **/
		public static final Codec<WhenApply> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.either(OrCase.CODEC, Case.CODEC).optionalFieldOf("when").forGetter(WhenApply::when),
				Codec.either(Model.CODEC.listOf(), Model.CODEC).xmap(
						either -> either.map(Function.identity(), List::of), // map list/singleton to list
						list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list)) // map list to list/singleton
					.fieldOf("apply").forGetter(WhenApply::apply)
			).apply(builder, WhenApply::new));
		
		/**
		 * Builder-like factory for datageneration, using a single-case when.
		 * @param when CaseDefinition, all of whose cases must be true to apply the model part.
		 * @param apply Model to apply when the 'when' is true for a given blockstate.
		 * @param additionalRandomModels optional additional Model(s) to include. One model from all models will be randomly chosen per block position.
		 * @return ModelSet builder
		 */
		public static WhenApply when(Case when, Model apply, Model... additionalRandomModels)
		{
			return new WhenApply(Optional.of(Either.right(when)), Lists.asList(apply, additionalRandomModels));
		}
		
		/**
		 * Builder-like factory for datageneration, using an OR-case when.
		 * @param when OrCase, any of whose cases must be true to apply the model part.
		 * @param apply Model to apply when the 'when' is true for a given blockstate.
		 * @param additionalRandomModels optional additional Model(s) to include. One model from all models will be randomly chosen per block position.
		 * @return WhenApply builder
		 */
		public static WhenApply or(OrCase when, Model apply, Model... additionalRandomModels)
		{
			return new WhenApply(Optional.of(Either.left(when)), Lists.asList(apply, additionalRandomModels));
		}
		
		/**
		 * Builder-like factory for datageneration, applying a Part to all blockstates.
		 * @param apply Model to apply to all blockstates.
		 * @param additionalRandomModels optional additional Model(s) to include. One model from all models will be randomly chosen per block position.
		 * @return WhenAPply builder
		 */
		public static WhenApply always(Model apply, Model... additionalRandomModels)
		{
			return new WhenApply(Optional.empty(), Lists.asList(apply, additionalRandomModels));
		}
	}
	
	/**
	 * Component of Multipart definitions representing a blockstate predicate.
	 * Each entry in this map is of the form "property": "values", using '|' to unionize multiple possible values,
	 * e.g. "side": "north|south|east".
	 * Each entry in the map must be true for a given blockstate for this case to allow
	 * a model to be applied to that state.
	 * 
	 * @param conditions Map of property-value conditions.
	 */
	public static record Case(Map<String,String> conditions)
	{
		/** codec **/
		public static final Codec<Case> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
			.xmap(Case::new, Case::conditions);
		
		/**
		 * Convenience method returning a single-property case
		 * @param <T> The property's value type, e.g. facing properties use {@link Direction}
		 * @param property Blockstate property, e.g. {@link BlockStateProperties#FACING}
		 * @param value A property value, e.g. {@link Direction#EAST}
		 * @param additionalValues optional additional values to add.
		 * Multipart allows multiple values per property in a given case,
		 * e.g. "east": "side|up" from redstone
		 * @return this
		 */
		@SafeVarargs
		public static <T extends Comparable<T>> Case create(Property<T> property, T value, T... additionalValues)
		{
			return Case.builder().addCondition(property, value, additionalValues);
		}
		
		/**
		 * Builder-like factory for convenience
		 * @return A CaseDefinition with mutable conditions
		 */
		public static Case builder()
		{
			return new Case(new HashMap<>());
		}
		
		/**
		 * Mutably adds a blockstate property condition for one or more property values to this case
		 * @param <T> The property's value type, e.g. facing properties use {@link Direction}
		 * @param property Blockstate property, e.g. {@link BlockStateProperties#FACING}
		 * @param value A property value, e.g. {@link Direction#EAST}
		 * @param additionalValues optional additional values to add.
		 * Multipart allows multiple values per property in a given case,
		 * e.g. "east": "side|up" from redstone
		 * @return this
		 */
		@SafeVarargs
		public final <T extends Comparable<T>> Case addCondition(Property<T> property, T value, T... additionalValues)
		{
			// multipart allows multiple values to be or'd with |, e.g. "east": "side|up"
			StringBuilder combinedValues = new StringBuilder(property.getName(value));
			for (T v : additionalValues)
			{
				combinedValues.append("|" + property.getName(v));
			}
			this.conditions.put(property.getName(), combinedValues.toString());
			return this;
		}
	}
	
	/**
	 * Component of Multipart definitions' 'when' blocks. Represents a union of blockstate predicates,
	 * where if any of the cases are true for a given state, then the models specified in the corresponding 'apply'
	 * block will be used for that blockstate.
	 * 
	 * @param cases List of Cases and/or OrCases to predicate blockstates with
	 */
	public static record OrCase(List<Either<OrCase, Case>> cases)
	{
		/** codec **/
		public static final Codec<OrCase> CODEC =
			Codec.either(ExtraCodecs.lazyInitializedCodec(() -> OrCase.CODEC), Case.CODEC)
				.listOf().fieldOf("OR").codec()
				.xmap(OrCase::new, OrCase::cases);
		
		/**
		 * Builder-like helper for datageneration
		 * @return OrCase with mutable list of conditions
		 */
		public static OrCase builder()
		{
			return new OrCase(new ArrayList<>());
		}
		
		/**
		 * Adds a case to this OrCase's list of cases (only usable if list is mutable)
		 * @param theCase Case to add
		 * @return this
		 */
		public OrCase addCase(Case theCase)
		{
			this.cases.add(Either.right(theCase));
			return this;
		}
		
		/**
		 * Adds an OrCase to this OrCase's list of cases (only usable if list is mutable)
		 * @param orCase OrCase to add
		 * @return this
		 */
		public OrCase addOrCase(OrCase orCase)
		{
			this.cases.add(Either.left(orCase));
			return this;
		}
	}
	
	/**
	 * Component representing a rotated model object in variant and multipart definitions.
	 * 
	 * @param model Model id, e.g. minecraft:block/dirt
	 * @param x Model x-rotation. Must be 0, 90, 180, or 270.
	 * @param y Model y-rotation. Must be 0, 90, 180, or 270.
	 * @param uvLock Whether to lock UVs when rotating model.
	 * @param weight Weight of model part when used in a list of model parts. Must be positive.
	 */
	public static record Model(ResourceLocation model, int x, int y, boolean uvLock, int weight)
	{
		/** codec **/
		public static final Codec<Model> CODEC = RecordCodecBuilder.<Model>create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("model").forGetter(Model::model),
				Codec.INT.optionalFieldOf("x",0).forGetter(Model::x),
				Codec.INT.optionalFieldOf("y",0).forGetter(Model::y),
				Codec.BOOL.optionalFieldOf("uvlock",false).forGetter(Model::uvLock),
				Codec.INT.optionalFieldOf("weight",1).forGetter(Model::weight)
			).apply(instance, Model::new));
		
		/**
		 * Component representing a rotated model object in variant and multipart definitions.
		 * 
		 * @param model Model id, e.g. minecraft:block/dirt
		 * @param x Model x-rotation. Must be 0, 90, 180, or 270.
		 * @param y Model y-rotation. Must be 0, 90, 180, or 270.
		 * @param uvLock Whether to lock UVs when rotating model.
		 * @param weight Weight of model part when used in a list of model parts. Must be positive.
		 */
		public Model
		{
			if (BlockModelRotation.by(x, y) == null)
				throw new IllegalArgumentException(String.format("Invalid blockstate model part rotation: x=%s, y=%s (must be 0, 90, 180, or 270)", x, y));
			if (weight < 1)
				throw new IllegalArgumentException(String.format("Invalid blockstate model part weight %s: weight must be positive", weight));
		}
		
		/**
		 * {@return new Model with no rotation, no uvlock, and weight 1}
		 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
		 */
		public static Model create(ResourceLocation model)
		{
			return create(model, BlockModelRotation.X0_Y0);
		}
		
		/**
		 * {@return new Model with no uvlock and weight 1}
		 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
		 * @param rotation x-y rotation to apply to the model
		 */
		public static Model create(ResourceLocation model, BlockModelRotation rotation)
		{
			return create(model, rotation, false);
		}
		
		/**
		 * {@return Model with weight 1}
		 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
		 * @param rotation x-y rotation to apply to the model
		 * @param uvLock whether to lock UVs
		 */
		public static Model create(ResourceLocation model, BlockModelRotation rotation, boolean uvLock)
		{
			return create(model, rotation, uvLock, 1);
		}
		
		/**
		 * {@return new Model}
		 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
		 * @param rotation x-y rotation to apply to the model
		 * @param uvLock whether to lock UVs
		 * @param weight weighting of positional-random Models, only used when multiple models are used in a variant/case
		 */
		public static Model create(ResourceLocation model, BlockModelRotation rotation, boolean uvLock, int weight)
		{
			int ordinal = rotation.ordinal();
			int x = (ordinal / 4) * 90;
			int y = (ordinal % 4) * 90;
			return new Model(model, x, y, uvLock, weight);
		}
	}
}