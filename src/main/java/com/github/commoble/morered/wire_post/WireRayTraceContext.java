package com.github.commoble.morered.wire_post;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

/** RayTraceContext but without requiring an entity **/
public class WireRayTraceContext
{
	public final Vec3d startVec;
	public final Vec3d endVec;
	public final RayTraceContext.BlockMode blockMode;
	public final RayTraceContext.FluidMode fluidMode;
	public final ISelectionContext context;

	public WireRayTraceContext(WireRayTraceSelectionContext selectionContext, Vec3d startVecIn, Vec3d endVecIn, RayTraceContext.BlockMode blockModeIn, RayTraceContext.FluidMode fluidModeIn)
	{
		this.startVec = startVecIn;
		this.endVec = endVecIn;
		this.blockMode = blockModeIn;
		this.fluidMode = fluidModeIn;
		this.context = selectionContext;
	}

	public Vec3d getEndVec()
	{
		return this.endVec;
	}

	public Vec3d getStartVec()
	{
		return this.startVec;
	}

	public VoxelShape getBlockShape(BlockState blockStateIn, IBlockReader worldIn, BlockPos pos)
	{
		return this.blockMode.get(blockStateIn, worldIn, pos, this.context);
	}

	public VoxelShape getFluidShape(IFluidState stateIn, IBlockReader worldIn, BlockPos pos)
	{
		return this.fluidMode.test(stateIn) ? stateIn.getShape(worldIn, pos) : VoxelShapes.empty();
	}

}
