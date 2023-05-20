package commoble.morered.wire_post;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;

/** ClipContext but without requiring an entity **/
public class WireRayTraceContext
{
	public final Vec3 from;
	public final Vec3 to;
	public final ClipContext.Block blockMode;
	public final ClipContext.Fluid fluidMode;
	public final CollisionContext context;

	public WireRayTraceContext(WireRayTraceSelectionContext selectionContext, Vec3 from, Vec3 to, ClipContext.Block blockModeIn, ClipContext.Fluid fluidModeIn)
	{
		this.from = from;
		this.to = to;
		this.blockMode = blockModeIn;
		this.fluidMode = fluidModeIn;
		this.context = selectionContext;
	}

	public Vec3 getTo()
	{
		return this.to;
	}

	public Vec3 getFrom()
	{
		return this.from;
	}

	public VoxelShape getBlockShape(BlockState blockStateIn, BlockGetter worldIn, BlockPos pos)
	{
		return this.blockMode.get(blockStateIn, worldIn, pos, this.context);
	}

	public VoxelShape getFluidShape(FluidState stateIn, BlockGetter worldIn, BlockPos pos)
	{
		return this.fluidMode.canPick(stateIn) ? stateIn.getShape(worldIn, pos) : Shapes.empty();
	}

}
