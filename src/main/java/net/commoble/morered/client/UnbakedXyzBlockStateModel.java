package net.commoble.morered.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;


public record UnbakedXyzBlockStateModel(ResourceLocation model, Quadrant x, Quadrant y, Quadrant z) implements CustomUnbakedBlockStateModel
{
	private static final ModelState[][][] XYZ_TRANSFORMS = Util.make(new ModelState[4][4][4], xyz -> {
		float radiansIncrement = (float) (Math.PI) * 0.5F;
		for (int x=0; x<4; x++)
		{
			for (int y=0; y<4; y++)
			{
				for (int z=0; z<4; z++)
				{
					if (z == 0)
					{
						xyz[x][y][z] = BlockModelRotation.by(Quadrant.values()[x], Quadrant.values()[y]);
					}
					else
					{
						Quaternionf q = new Quaternionf();
						q.mul(new Quaternionf().setAngleAxis(-z*radiansIncrement, 0F, 0F, 1F));
						q.mul(new Quaternionf().setAngleAxis(-y*radiansIncrement, 0F, 1F, 0F));
						q.mul(new Quaternionf().setAngleAxis(-x*radiansIncrement, 1F, 0F, 0F));
						xyz[x][y][z] = new TransformationModelState(new Transformation((Vector3f)null, q, (Vector3f)null, (Quaternionf)null));	
					}
				}
			}
		}
	});
	
	public static final MapCodec<UnbakedXyzBlockStateModel> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			ResourceLocation.CODEC.fieldOf("model").forGetter(UnbakedXyzBlockStateModel::model),
			Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(UnbakedXyzBlockStateModel::x),
			Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(UnbakedXyzBlockStateModel::y),
			Quadrant.CODEC.optionalFieldOf("z", Quadrant.R0).forGetter(UnbakedXyzBlockStateModel::z)
		).apply(builder, UnbakedXyzBlockStateModel::new));

	@Override
	public MapCodec<? extends CustomUnbakedBlockStateModel> codec()
	{
		return CODEC;
	}

	@Override
	public void resolveDependencies(Resolver resolver)
	{
		resolver.markDependency(this.model);
	}

	@Override
	public BlockStateModel bake(ModelBaker baker)
	{
		return new SingleVariant(SimpleModelWrapper.bake(baker, this.model, XYZ_TRANSFORMS[this.x.ordinal()][this.y.ordinal()][this.z.ordinal()]));
	}
}
