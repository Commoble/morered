package commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;

public class BundledCablePostBlock extends AbstractCablePostBlock
{

	public BundledCablePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter)
	{
		super(properties, connectionGetter);
	}

}
