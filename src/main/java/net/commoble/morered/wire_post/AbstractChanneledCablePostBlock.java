package net.commoble.morered.wire_post;

import java.util.Map;

import net.commoble.morered.future.Channel;
import net.commoble.morered.future.TransmissionNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractChanneledCablePostBlock extends AbstractPostBlock
{

	protected static final VoxelShape[] CABLE_POST_SHAPES_DUNSWE = {
		Block.box(4D, 0D, 4D, 12D, 12D, 12D),
		Block.box(4D, 4D, 4D, 12D, 16D, 12D),
		Block.box(4D, 4D, 0D, 12D, 12D, 12D),
		Block.box(4D, 4D, 4D, 12D, 12D, 16D),
		Block.box(0D, 4D, 4D, 12D, 12D, 12D),
		Block.box(4D, 4D, 4D, 16D, 12D, 12D)
	};
	
	public AbstractChanneledCablePostBlock(Properties properties)
	{
		super(properties);
	}

//	@Override
//	protected void notifyNeighbors(Level level, BlockPos pos, BlockState state)
//	{
//		// markdirty is sufficient for notifying neighbors of internal BE updates
//		// standard block updates are sufficient for notifying neighbors of blockstate addition/removal
//		// but we do want to notify connected BEs
//		if (level.getBlockEntity(pos) instanceof WirePostBlockEntity be)
//		{
//			be.notifyConnections();
//		}
//	}

	@Override
	public Map<Channel, TransmissionNode> getTransmissionNodes(BlockGetter level, BlockPos pos, BlockState state, Direction face)
	{
		return Map.of();
	}
	
	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
	{
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		this.updatePower(worldIn,pos);
	}

	@Override
	public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor)
	{
		super.onNeighborChange(state, world, pos, neighbor);
		this.updatePower(world,pos);
	}
	
	protected void updatePower(BlockGetter world, BlockPos pos)
	{
		BundledCablePostBlockEntity.getCablePost(world, pos).ifPresent(te -> te.updatePower());
	}

	
}
