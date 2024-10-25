package net.commoble.morered.transportation;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RedstoneTubeBlock extends TubeBlock
{
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public RedstoneTubeBlock(ResourceLocation textureLocation, Properties properties)
	{
		super(textureLocation, properties);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(POWERED, false));
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(POWERED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().redstoneTubeEntity.get().create(pos, state);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return type == MoreRed.get().redstoneTubeEntity.get()
			? (BlockEntityTicker<T>) TubeBlockEntity.TICKER
			: super.getTicker(level, state, type);
	}

	/**
	 * @deprecated call via
	 *             {@link IBlockState#getWeakPower(IBlockAccess,BlockPos,EnumFacing)}
	 *             whenever possible. Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
	{
		return blockState.getValue(POWERED) ? 15 : 0;
	}

	/**
	 * @deprecated call via
	 *             {@link IBlockState#getStrongPower(IBlockAccess,BlockPos,EnumFacing)}
	 *             whenever possible. Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
	{
		return blockState.getValue(POWERED) ? 15 : 0;
	}

	/**
	 * Can this block provide power. Only wire currently seems to have this change
	 * based on its state.
	 * 
	 * @deprecated call via {@link IBlockState#canProvidePower()} whenever possible.
	 *             Implementing/overriding is fine.
	 */
	@Deprecated
	@Override
	public boolean isSignalSource(BlockState state)
	{
		return true;
	}
}
