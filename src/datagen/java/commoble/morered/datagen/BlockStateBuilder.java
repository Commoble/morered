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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

/**
 * Alternative datageneration for blockstate jsons. Usable with {@link JsonCodecProvider}.
 */
public final class BlockStateBuilder
{
	private BlockStateBuilder() {}
	
	/**
	 * Specifies a blockstate file with a variants definition. See {@link Variants#builder}.
	 * @param variants Variants mapping sets of blockstates to individual models.
	 * @return BlockStateDefinition for datageneration.
	 */
	public static BlockModelDefinition variants(Variants variants)
	{
		return new BlockModelDefinition(Optional.of(variants.toVanilla()), Optional.empty());
	}
	
	/**
	 * Specifies a blockstate file with a multipart definition. See {@link Multipart#builder}.
	 * @param multipart Multipart definition assigning (possibly multiple) models to states.
	 * @return BlockStateDefinition for datageneration.
	 */
	public static BlockModelDefinition multipart(Multipart multipart)
	{
		return new BlockModelDefinition(Optional.empty(), Optional.of(multipart.toVanilla()));
	}
	
	/**
	 * Specifies a blockstate file with both variants and multipart definitions. Variants override multiparts.
	 * @param variants Variants mapping sets of blockstates to individual models.
	 * @param multipart Multipart definition assigning (possibly multiple) models to states.
	 * @return BlockStateDefinition for datageneration
	 */
	public static BlockModelDefinition variantsAndMultipart(Variants variants, Multipart multipart)
	{
		return new BlockModelDefinition(Optional.of(variants.toVanilla()), Optional.of(multipart.toVanilla()));
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
	
	private static Condition vanillifyCondition(Either<OrCase, Case> when)
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
	 * @param variants Map of blockstate property predicates to models
	 */
	public static record Variants(Map<List<PropertyValue<?>>, BlockStateModel.Unbaked> variants)
	{
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
		 * @param model Unbaked BlockStateModel to use
		 * @return new Variants which applies the given model to all blockstates
		 */
		public static Variants always(BlockStateModel.Unbaked model)
		{
			return Variants.builder().addVariant(List.of(), model);
		}
		
		/**
		 * Adds a variant for your blockstate json
		 * @param value e.g. "facing=east"
		 * @param model BlockStateModel.Unbaked, e.g. { "model": "minecraft:block/acacia_stairs_inner" } 
		 * @return this
		 */
		public Variants addVariant(PropertyValue<?> value, BlockStateModel.Unbaked model)
		{
			this.addVariant(List.of(value), model);
			return this;
		}
		
		/**
		 * Adds a variant for your blockstate json
		 * @param values e.g. "facing=east,powered=false"
		 * @param model Model definition, e.g. { "model": "minecraft:block/acacia_stairs_inner" }.
		 * @return this
		 */
		public Variants addVariant(List<PropertyValue<?>> values, BlockStateModel.Unbaked model)
		{
			this.variants.put(values, model);
			return this;
		}
		
		public BlockModelDefinition.SimpleModelSelectors toVanilla()
		{
			Map<String, BlockStateModel.Unbaked> selectors = new HashMap<>();
			variants.forEach((propertyValues,model) -> {
				selectors.put(
					String.join(",", propertyValues.stream().map(PropertyValue::toString).toList()),
					model);
			});
			return new BlockModelDefinition.SimpleModelSelectors(selectors);
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
		
		public BlockModelDefinition.MultiPartDefinition toVanilla()
		{
			List<Selector> selectors = new ArrayList<>();
			
			for (var whenApply : cases)
			{
				selectors.add(new Selector(
					whenApply.when.map(when -> vanillifyCondition(when)),
					whenApply.apply));
			}
			
			return new BlockModelDefinition.MultiPartDefinition(selectors);
		}
	}
	
	/**
	 * Half-built builder for a WhenApply, specifying the blockstate conditions but not the model(s)
	 * @param when Case, OrCase, or always-case if empty
	 */
	public static record When(Optional<Either<OrCase,Case>> when)
	{
		/**
		 * {@return When builder for a Case}
		 * @param when Case determining which blockstates to apply model parts to
		 */
		public static When when(Case when)
		{
			return new When(Optional.of(Either.right(when)));
		}
		
		/**
		 * {@return When builder for an OrCase}
		 * @param when OrCase determining which blockstates to apply model parts to
		 */
		public static When or(OrCase when)
		{
			return new When(Optional.of(Either.left(when)));
		}
		
		/**
		 * {@return When builder which will apply model to all blockstates}
		 */
		public static When always()
		{
			return new When(Optional.empty());
		}
		
		/**
		 * {@return WhenApply applying a single model to the specified blockstates}
		 * @param apply Unbaked BlockStateModel to apply to the specified blockstates
		 */
		public WhenApply apply(BlockStateModel.Unbaked apply)
		{
			return new WhenApply(this.when, apply);
		}
		
		/**
		 * {@return WhenApply with random weighted models
		 * @param firstModel Weighted Unbaked BlockStateModel
		 * @param secondModel Weighted Unbaked BlockStateModel
		 * @param additionalModels additional models as needed
		 */
		@SafeVarargs
		public final WhenApply applyRandomModels(Weighted<BlockStateModel.Unbaked> firstModel, Weighted<BlockStateModel.Unbaked> secondModel, Weighted<BlockStateModel.Unbaked>... additionalModels)
		{
			return new WhenApply(this.when, new WeightedVariants.Unbaked(WeightedList.of(Lists.asList(firstModel, secondModel, additionalModels))));
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
	 * @param apply Unbaked BlockStateModel to apply for the given blockstate(s)
	 */
	public static record WhenApply(Optional<Either<OrCase, Case>> when, BlockStateModel.Unbaked apply)
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
	public static record Case(Map<String,String> conditions)
	{		
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
}