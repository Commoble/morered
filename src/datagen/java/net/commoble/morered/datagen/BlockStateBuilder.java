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
package net.commoble.morered.datagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Quadrant;

import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.CombinedCondition;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Alternative datageneration for blockstate jsons
 * Use {@link BlockStateBuilder#singleVariant}, {@link BlockStateBuilder#variants}, or {@link BlockStateBuilder#multipart}
 * to build a BlockModelDefinition (the java representation of a blockstate json).
 * 
 * {@link BlockStateBuilder#model} or its overloads can help create BlockStateModel.Unbakeds (the json objects with model, x, y, uvlock)
 * when the builders ask for one.
 * 
 * {@link BlockStateBuilder#addDataProvider(GatherDataEvent, Map)} is a shortcut for creating the dataprovider from a map of BlockModelDefinitions.
 */
public final class BlockStateBuilder
{
	private BlockStateBuilder() {}
	
	/**
	 * Helper to add a dataprovider for blockstate jsons
	 * @param event GatherDataEvent
	 * @param blockStates Map of BlockModelDefinitions (blockstate jsons) by id.
	 * Use {@link BlockStateBuilder#singleVariant}, {@link BlockStateBuilder#variants}, or {@link BlockStateBuilder#multipart} to create
	 * BlockModelDefinitions to add to this map, or {@link BlockStateBuilder#custom} for non-vanilla implementations
	 */
	public static void addDataProvider(GatherDataEvent event, Map<ResourceLocation, BlockModelDefinition> blockStates)
	{
		DataGenerator generator = event.getGenerator();
		generator.addProvider(true, JsonDataProvider.create(event.getLookupProvider(), generator.getPackOutput(), generator, PackOutput.Target.RESOURCE_PACK, "blockstates", BlockModelDefinition.CODEC, blockStates));
	}
	
	/**
	 * Creates and returns a BlockModelDefinition for a variants-type blockstate json which assigns one model to all states.
	 * @param model Unbaked BlockStateModel to use, e.g. {@snippet :
	 * {
	 *   "model": "foo:bar",
	 *   "x": 90,
	 *   "y": 180,
	 *   "uvlock": true
	 * }
	 * }see {@link BlockStateBuilder#model} or its overloads for helpers to create vanilla models
	 * (or a CustomUnbakedBlockStateModel can be used by constructing it yourself)
	 * @return BlockModelDefinition for a blockstate json of the form {@snippet :
	 * {
	 *   "variants": {
	 *     "": {"model": "foo:bar"}
	 * 	}
	 * }
	 */
	public static BlockModelDefinition singleVariant(BlockStateModel.Unbaked model)
	{
		return variants(variant -> variant.addMultiPropertyVariant(propertyValues -> {}, model));
	}
	
	/**
	 * Builds and returns a BlockModelDefinition for a variants blockstate json.
	 * @param variantsBuilder Variants builder which cases can be added too, see Variants javadoc for details
	 * @return BlockModelDefinition for a blockstate json of the form {@snippet :
	 * {
	 * 	"variants": {
	 *    "powered=false": {"model": "foo:bar"},
	 *    "powered=true": {"model": "foo:bar_powered"}
	 *  }
	 * }
	 */
	public static BlockModelDefinition variants(Consumer<Variants> variantsBuilder)
	{
		Variants variants = Variants.builder();
		variantsBuilder.accept(variants);
		return new BlockModelDefinition(Optional.of(bakeVariants(variants)), Optional.empty());
	}
	
	/**
	 * Builds and returns a BlockModelDefinition for a multipart blockstate json.
	 * @param multipartBuilder Multipart builder which when-apply cases can be added to, see Multipart javadoc for details
	 * @return BlockModelDefinition for a blockstate json of the form {@snippet :
	 * {
	 *   "multipart": [
	 *     {
	 *       "when": {"powered": false},
	 *       "apply": {"model": "foo:bar"}
	 *     }
	 *   ]
	 * } 
	 */
	public static BlockModelDefinition multipart(Consumer<Multipart> multipartBuilder)
	{
		Multipart multipart = Multipart.builder();
		multipartBuilder.accept(multipart);
		return new BlockModelDefinition(Optional.empty(), Optional.of(bakeMultipart(multipart)));
	}
	
	/**
	 * Builds and returns a BlockModelDefinition which has both variants and multipart
	 * @param variantsBuilder Variants builder
	 * @param multipartBuilder Multipart builder
	 * @return BlockModelDefinition for a blockstate json which has both variants and multipart
	 */
	public static BlockModelDefinition variantsAndMultipart(Consumer<Variants> variantsBuilder, Consumer<Multipart> multipartBuilder)
	{
		Variants variants = Variants.builder();
		variantsBuilder.accept(variants);
		Multipart multipart = Multipart.builder();
		multipartBuilder.accept(multipart);
		return new BlockModelDefinition(Optional.of(bakeVariants(variants)), Optional.of(bakeMultipart(multipart)));		
	}
	
	/**
	 * Creates a non-vanilla BlockModelDefinition
	 * @param customBlockModelDefinition CustomBlockModelDefinition (not variants or multipart)
	 * @return BlockModelDefinition for blockstate json with non-vanilla format
	 */
	public static BlockModelDefinition custom(CustomBlockModelDefinition customBlockModelDefinition)
	{
		return new BlockModelDefinition(customBlockModelDefinition);
	}
	
	private static BlockModelDefinition.SimpleModelSelectors bakeVariants(Variants variants)
	{
		return new BlockModelDefinition.SimpleModelSelectors(variants.variants.entrySet().stream().collect(Collectors.toMap(
			entry -> String.join(",", entry.getKey().stream().map(PropertyValue::toString).toList()),
			entry -> entry.getValue())));
	}
	
	private static BlockModelDefinition.MultiPartDefinition bakeMultipart(Multipart multipart)
	{
		return new BlockModelDefinition.MultiPartDefinition(multipart.cases.stream().map(whenApply -> new Selector(
			whenApply.when.map(BlockStateBuilder::vanillifyCondition),
			whenApply.apply))
			.toList());
	}
	
	/**
	 * {@return new Unbaked BlockStateModel with no rotation, no uvlock}
	 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
	 */
	public static BlockStateModel.Unbaked model(ResourceLocation model)
	{
		return model(model, Quadrant.R0, Quadrant.R0);
	}
	
	/**
	 * {@return new Unbaked BlockStateModel with specified rotation and no uvlock}
	 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
	 * @param x x-rotation to apply to the model
	 * @param y y-rotation to apply to the model
	 */
	public static BlockStateModel.Unbaked model(ResourceLocation model, Quadrant x, Quadrant y)
	{
		return model(model, x, y, false);
	}
	
	/**
	 * {@return new Unbaked BlockStateModel with specified rotation and uvlock}
	 * @param model ResourceLocation of a model, e.g. "minecraft:block/dirt"
	 * @param x x-rotation to apply to the model
	 * @param y y-rotation to apply to the model
	 * @param uvLock whether to lock UVs
	 */
	public static BlockStateModel.Unbaked model(ResourceLocation model, Quadrant x, Quadrant y, boolean uvLock)
	{
		return new SingleVariant.Unbaked(new Variant(model)
			.withXRot(x)
			.withYRot(y)
			.withUvLock(uvLock));
	}
	
	/**
	 * {@return new Unbaked BlockStateModel for random selection from a list of two or more Weighted models (e.g. like how stone is)}
	 * @param firstModel Weighted BlockStateModel.Unbaked, e.g. new Weighted<>(BlockStateBuilder.model(ResourceLocation.fromNamespaceAndPath("foo","bar")), 1)
	 * @param secondModel another weighted model
	 * @param additionalModels more weighted models as needed
	 */
	@SafeVarargs
	public static BlockStateModel.Unbaked randomModels(Weighted<BlockStateModel.Unbaked> firstModel, Weighted<BlockStateModel.Unbaked> secondModel, Weighted<BlockStateModel.Unbaked>... additionalModels)
	{
		return new WeightedVariants.Unbaked(WeightedList.of(Lists.asList(firstModel, secondModel, additionalModels)));
	}
	
	private static Condition vanillifyCondition(Either<OrWhen, When> when)
	{
		return when.<Condition>map(
			orCase -> new CombinedCondition(
				CombinedCondition.Operation.OR,
				orCase.cases.stream().map(either -> vanillifyCondition(either)).toList()),
			oneCase -> {
				Map<String, KeyValueCondition.Terms> tests = new HashMap<>();
				
				oneCase.conditions.forEach((key,value) -> KeyValueCondition.Terms.parse(value)
					.resultOrPartial(e -> {
						throw new RuntimeException(e);
					})
					.ifPresent(result -> tests.put(key,result)));
				
				return new KeyValueCondition(tests);
			});
	}

	/**
	 * Represents a "variants" block in a blockstate json.
	 * Can be used as a builder like so: {@snippet :
	 * BlockStateBuilder.variants(variants -> variants
	 *   .addVariant(BlockStateProperties.POWERED, false, BlockStateBuilder.model(ResourceLocation.fromNamespaceAndPath("foo, "bar")))
	 *   .addVariant(propertyValues -> propertyValues
	 *     .addPropertyValue(BlockStateProperties.POWERED, true)
	 *     .addPropertyValue(BlockStateProperties.LIT, false),
	 *     BlockStateBuilder.model(ResourceLocation.fromNamespaceAndPath("foo, "bar_powered"))));
	 * }
	 * Which results in the blockstate json {@snippet :
	 * {
	 *   "variants": {
	 *     "powered=false": {"model": "foo:bar"},
	 *     "powered=true,lit=false": {"model": "foo:bar_powered"}
	 *   }
	 * }
	 * }
	 * @param variants Map of blockstate property predicates to models
	 */
	public static record Variants(Map<List<PropertyValue<?>>, BlockStateModel.Unbaked> variants)
	{
		private static Variants builder()
		{
			return new Variants(new HashMap<>());
		}
		
		/**
		 * Adds a single-property variant to your blockstate json., e.g. "powered=true": {"model": "foo:bar"}
		 * @param <T> type of the blockstate property to filter blockstates by
		 * @param property blockstate Property to filter blockstates by, e.g. {@link BlockStateProperties#POWERED}
		 * @param value value of the blockstate Property to filter blockstates by, e.g. true
		 * @param model BlockStateModel.Unbaked to assign to blockstates which have that property value,
		 * see {@link BlockStateBuilder#model} or its overloads to help create the model
		 * @return this
		 */
		public <T extends Comparable<T>> Variants addVariant(Property<T> property, T value, BlockStateModel.Unbaked model)
		{
			this.variants.put(List.of(new PropertyValue<>(property,value)), model);
			return this;
		}
		
		/**
		 * Adds a multi-property variant to your blockstate json, e.g. "powered=true,lit=false": {"model": "foo:bar"}
		 * @param propertyValuesBuilder PropertyValueList builder to add property values to, see its javadoc for details
		 * @param model BlockStateModel.Unbaked to assign to blockstates which have all specified property values,
		 * see {@link BlockStateBuilder#model} or its overloads to help create the model
		 * @return this
		 */
		public Variants addMultiPropertyVariant(Consumer<PropertyValueList> propertyValuesBuilder, BlockStateModel.Unbaked model)
		{
			PropertyValueList list = PropertyValueList.builder();
			propertyValuesBuilder.accept(list);
			this.variants.put(list.propertyValues, model);
			return this;
		}
	}
	
	/**
	 * Component of Variants definitions, representing a property-value entry, e.g. "powered=true"
	 * 
	 * @param property Property of a blockstate.
	 * @param value value of a blockstate Property.
	 */
	private static record PropertyValue<T extends Comparable<T>>(Property<T> property, T value)
	{		
		@Override
		public String toString()
		{
			return this.property.getName() + "=" + this.property.getName(value);
		}
	}
	
	/**
	 * Component of Variants definitions, representing one or more property-value entries, e.g. "powered=true,lit=false"
	 */
	public static record PropertyValueList(List<PropertyValue<?>> propertyValues)
	{
		private static PropertyValueList builder()
		{
			return new PropertyValueList(new ArrayList<>());
		}
		
		/**
		 * Adds a blockstate property value to this variant's state filter, e.g. "powered=true"
		 * @param <T> type of the blockstate property to filter blockstates by
		 * @param property blockstate Property to filter blockstates by, e.g. {@link BlockStateProperties#POWERED}
		 * @param value value of the blockstate Property to filter blockstates by, e.g. true
		 * @return this
		 */
		public <T extends Comparable<T>> PropertyValueList addPropertyValue(Property<T> property, T value)
		{
			this.propertyValues.add(new PropertyValue<>(property, value));
			return this;
		}
	}
	
	/**
	 * Multipart format for blockstate jsons. Assigns models to blockstates by comparing states to property-value
	 * predicate Cases. Multiple models can be applied to individual blockstates. When used with the Variants format,
	 * models applied by Variants will override models applied by Multiparts.
	 * 
	 * Can be used as a builder like so: {@snippet :
	 * BlockStateBuilder.multipart(multipart -> multipart
	 *   .apply(BlockStateBuilder.model(ResourceLocation.fromNamespaceAndPath("foo", "bar")))
	 *   .applyWhen(BlockStateBuilder.model(ResourceLocation.fromNamespaceAndPath("foo", "bar_powered")), BlockStateProperties.POWERED, true)
	 *   .applyWhenAll(BlockStateBuilder.model(ResourceLocation.fromNamespaceAndPath("foo", "bar_lit")), when -> when
	 *     .addCondition(BlockStateProperties.POWERED, false)
	 *     .addCondition(BlockStateProperties.LIT, true)));
	 * }
	 * Which results in the json: {@snippet :
	 * {
	 *   "multipart": [
	 *     {
	 *       "apply": {"model": "foo:bar"},
	 *     },
	 *     {
	 *       "when": {"powered": "true"},
	 *       "apply": {"model": "foo:bar_powered"}
	 *     },
	 *     {
	 *       "when": {"powered": "false", "lit": "true"},
	 *       "apply": {"model": "foo:bar_lit"}
	 *     }
	 * 
	 * @param cases List of when-apply components.
	 */
	public static record Multipart(List<WhenApply> cases)
	{
		private static Multipart builder()
		{
			return new Multipart(new ArrayList<>());
		}
		
		/**
		 * Applies a model to all blockstates, e.g. {"apply": {"model": "foo:bar"}}
		 * @param apply BlockStateModel.Unbaked to apply to all blockstates, see {@link BlockStateBuilder#model} or its overloads to help create these
		 * @return this
		 */
		public Multipart apply(BlockStateModel.Unbaked apply)
		{
			this.cases.add(new WhenApply(Optional.empty(), apply));
			return this;
		}
		
		/**
		 * Applies a model to blockstates which have any specified value of the specified property, e.g. {@snippet :
		 * {
		 *   "when": {
		 *     "powered": "true"
		 *   },
		 *   "apply": {"model": "foo:bar"}
		 * }}
		 * @param <T> type of the blockstate property's values, e.g. Boolean
		 * @param apply BlockStateModel.Unbaked to apply to blockstates which match the criteria
		 * @param property blockstate Property to filter states by, e.g. BlockStateProperties.POWERED
		 * @param value blockstate Property value to filter states by, e.g. true
		 * @param additionalValues additional blockstate property values which the state can match any of
		 * @return this
		 */
		@SafeVarargs
		public final <T extends Comparable<T>> Multipart applyWhen(BlockStateModel.Unbaked apply, Property<T> property, T value, T... additionalValues)
		{
			return this.applyWhenAll(apply, when -> {
				when.addCondition(property, value, additionalValues);
			});
		}
		
		/**
		 * Applies a model to blockstates which match all property-values specified via the provided When builder, with results like: {@snippet :
		 *   "when": {
		 *     "lit": "false",
		 *     "powered": "true"
		 *   },
		 *   "apply": {"model": "foo:bar"}
		 * }
		 * @param apply BlockStateModel.Unbaked to apply to blockstates which match all property-values added to the When
		 * @param whenBuilder When to add property values to, see When javadocs for details
		 * @return this
		 */
		public Multipart applyWhenAll(BlockStateModel.Unbaked apply, Consumer<When> whenBuilder)
		{
			When theCase = When.builder();
			whenBuilder.accept(theCase);
			this.cases.add(new WhenApply(Optional.of(Either.right(theCase)), apply));
			return this;
		}

		/**
		 * Applies a model to blockstates which match any specified cases specified via the provided OrWhen builder
		 * @param apply BlockStateModel.Unbaked to apply to blockstates which match any property values added to the OrWhen
		 * @param orWhenBuilder OrWhen consumer to add property values to, see OrWhen javadocs for details
		 * @return this
		 */
		public Multipart applyWhenOr(BlockStateModel.Unbaked apply, Consumer<OrWhen> orWhenBuilder)
		{
			OrWhen orWhen = OrWhen.builder();
			orWhenBuilder.accept(orWhen);
			this.cases.add(new WhenApply(Optional.of(Either.left(orWhen)), apply));
			return this;
		}
	}
	
	/**
	 * Component of Multipart definitions, representing the "when" and "apply" blocks
	 * in a blockstate json.
	 * 
	 * @param when Optional field specifying one or more when predicates to apply to each blockstate.
	 * If a When is provided, the model will be used for blockstates that match that case.
	 * If an OrWhen is provided, the model will be used for blockstates that match any of the sub-cases.
	 * If no When is provided, the model will be used for all blockstates. 
	 * @param apply Unbaked BlockStateModel to apply for the given blockstate(s)
	 */
	private static record WhenApply(Optional<Either<OrWhen, When>> when, BlockStateModel.Unbaked apply)
	{
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
	public static record When(Map<String,String> conditions)
	{
		/**
		 * Builder-like factory
		 * @return A When with mutable conditions
		 */
		private static When builder()
		{
			return new When(new HashMap<>());
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
		public final <T extends Comparable<T>> When addCondition(Property<T> property, T value, T... additionalValues)
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
	 * @param cases List of Whens and/or OrWhens to predicate blockstates with
	 */
	public static record OrWhen(List<Either<OrWhen, When>> cases)
	{
		private static OrWhen builder()
		{
			return new OrWhen(new ArrayList<>());
		}
		
		/**
		 * Adds a single-property subcase to this OrWhen
		 * @param <T> type of the property's values, e.g. Boolean
		 * @param property blockstate Property to filter by, e.g. BlockStateProperties.POWERED
		 * @param value value of the blockstate Property to filter by, e.g. true
		 * @param additionalValues additional values blockstates can have
		 * @return this
		 */
		@SafeVarargs
		public final <T extends Comparable<T>> OrWhen addSinglePropertySubWhen(Property<T> property, T value, T... additionalValues)
		{
			return this.addAllPropertiesSubWhen(caseBuilder -> caseBuilder.addCondition(property, value, additionalValues));
		}
		
		/**
		 * Adds a multi-property subcase to this OrWhen; all properties added must match for the subcase to be true
		 * @param caseBuilder When builder
		 * @return this
		 */
		public OrWhen addAllPropertiesSubWhen(Consumer<When> caseBuilder)
		{
			When theCase = When.builder();
			caseBuilder.accept(theCase);
			this.cases.add(Either.right(theCase));
			return this;
		}
		
		/**
		 * Adds an OrWhen sub-case to this OrWhen
		 * @param orBuilder OrWhen builder
		 * @return this
		 */
		public OrWhen addOrSubWhen(Consumer<OrWhen> orBuilder)
		{
			OrWhen orCase = OrWhen.builder();
			orBuilder.accept(orCase);
			this.cases.add(Either.left(orCase));
			return this;
		}
	}
}