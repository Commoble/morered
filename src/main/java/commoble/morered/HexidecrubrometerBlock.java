package commoble.morered;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;

public class HexidecrubrometerBlock extends Block
{
	public static final IntegerProperty POWER = BlockStateProperties.POWER_0_15;
	public static final EnumProperty<AttachFace> READING_FACE = BlockStateProperties.FACE;
	
	// when horizontal, this is the direction that the display faces
	// when vertical, this is the direction that the bottom of the number faces
	public static final EnumProperty<Direction> ROTATION = BlockStateProperties.HORIZONTAL_FACING;
	

	public HexidecrubrometerBlock(Properties properties)
	{
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState()
			.with(POWER,0)
			.with(READING_FACE, AttachFace.FLOOR)
			.with(ROTATION, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		return super.getStateForPlacement(context);
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder)
	{
		super.fillStateContainer(builder);
		builder.add(POWER, READING_FACE, ROTATION);
	}
	
	

}
