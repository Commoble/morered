package commoble.morered.wire_post;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

/**
 * IBlockReader that delegates to another reader for most purposes,
 * except that a state at a given position will appear to be a different given state
 */
public class FakeStateWorld implements IBlockReader
{
	private final IBlockReader delegate;
	private final BlockPos pos;
	private final BlockState state;
	
	public FakeStateWorld(IBlockReader delegate, BlockPos pos, BlockState state)
	{
		this.delegate = delegate;
		this.pos = pos.toImmutable();
		this.state = state;
	}

	@Override
	public TileEntity getTileEntity(BlockPos pos)
	{
		return this.delegate.getTileEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos)
	{
		return pos.equals(this.pos) ? this.state : this.delegate.getBlockState(pos); 
	}

	@Override
	public FluidState getFluidState(BlockPos pos)
	{
		return this.delegate.getFluidState(pos);
	}
	
}
