package commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;

public abstract class AbstractCablePostBlock extends AbstractWirePostBlock
{

	protected static final VoxelShape[] CABLE_POST_SHAPES_DUNSWE = {
		Block.makeCuboidShape(4D, 0D, 4D, 12D, 12D, 12D),
		Block.makeCuboidShape(4D, 16D, 4D, 12D, 4D, 12D),
		Block.makeCuboidShape(4D, 4D, 0D, 12D, 12D, 12D),
		Block.makeCuboidShape(4D, 4D, 4D, 12D, 12D, 16D),
		Block.makeCuboidShape(0D, 4D, 4D, 12D, 12D, 12D),
		Block.makeCuboidShape(4D, 4D, 4D, 16D, 12D, 12D)
	};
	
	public AbstractCablePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties, connectionGetter);
	}

	
}
