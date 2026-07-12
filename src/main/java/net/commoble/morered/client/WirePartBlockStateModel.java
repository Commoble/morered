package net.commoble.morered.client;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.morered.wires.WireModelData;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public record UnbakedWirePartBlockStateModel(Identifier line, Identifier edge) implements CustomUnbakedBlockStateModel
{
	public static final MapCodec<UnbakedWirePartBlockStateModel> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Identifier.CODEC.fieldOf("line").forGetter(UnbakedWirePartBlockStateModel::line),
			Identifier.CODEC.fieldOf("edge").forGetter(UnbakedWirePartBlockStateModel::edge)
		).apply(builder, UnbakedWirePartBlockStateModel::new));
	
	@Override
	public BlockStateModel bake(ModelBaker baker)
	{
		BlockStateModelPart[] lineModels = new BlockStateModelPart[24];
		BlockStateModelPart[] edgeModels = new BlockStateModelPart[12];
		
		int materialFlags = 0;
		
		// for each combination of attachment face + rotated connection side, bake a connection line model
		for (int side = 0; side < 6; side++)
		{
			for (int subSide = 0; subSide < 4; subSide++)
			{
				int index = side*4 + subSide;
				ModelState transform = FaceRotation.getFaceRotation(side, subSide);
				BlockStateModelPart part = SimpleModelWrapper.bake(baker, this.line, transform);
				lineModels[index] = part;
				materialFlags |= part.materialFlags();
			}
		}
		
		for (int edge = 0; edge < 12; edge++)
		{
			// the 12 edges are a little weirder to index
			// let's define them in directional precedence
			// down comes first, then up, then the sides
			// the "default" edge with no rotation has to be on the middle sides to ignore the z-axis, we'll use bottom-west
			ModelState transform = EdgeRotation.EDGE_ROTATIONS[edge];
			BlockStateModelPart part = SimpleModelWrapper.bake(baker, this.edge, transform);
			edgeModels[edge] = part;
			materialFlags |= part.materialFlags();
		}
		
		return new WirePartBlockStateModel(lineModels, edgeModels, lineModels[0].particleMaterial(), materialFlags);
	}

	@Override
	public void resolveDependencies(Resolver resolver)
	{
		resolver.markDependency(this.line);
		resolver.markDependency(this.edge);
	}

	@Override
	public MapCodec<? extends CustomUnbakedBlockStateModel> codec()
	{
		return CODEC;
	}
	
	public static record WirePartBlockStateModel(
		BlockStateModelPart[] lineModels,
		BlockStateModelPart[] edgeModels,
		Material.Baked particleMaterial,
		int materialFlags // @Overrides materialFlags() from base interfaces
	) implements DynamicBlockStateModel
	{
		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts)
		{
			Long wireData = level.getModelData(pos).get(WireModelData.PROPERTY);
			if (wireData == null || wireData == 0)
				return;
			long wireFlags = wireData;

			int lineStart = 6;
			int lines = 24;
			int edgeStart = lineStart+lines;
			int edges = 12;
			for (int i=0; i<lines; i++)
			{
				if (WireModelData.test(wireFlags, i+lineStart))
					parts.add(this.lineModels[i]);
			}
			for (int i=0; i < edges; i++)
			{
				if (WireModelData.test(wireFlags, i+edgeStart))
					parts.add(this.edgeModels[i]);
			}
		}

		@Override
		public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random)
		{
			return new GeometryKey(state, Objects.requireNonNullElse(level.getModelData(pos).get(WireModelData.PROPERTY), 0L));
		}
	}
	
	private static record GeometryKey(BlockState state, long modelData) {}
}