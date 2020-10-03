package commoble.morered.wire_post;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.EntitySelectionContext;

public class WireRayTraceSelectionContext extends EntitySelectionContext
{
	private final Set<BlockPos> ignoreSet;
	
	public WireRayTraceSelectionContext(BlockPos start, BlockPos end)
	{
		super(false, -Double.MAX_VALUE, Items.AIR); // same as EntitySelectionContext.DUMMY
		this.ignoreSet = ImmutableSet.of(start.toImmutable(), end.toImmutable());
	}
	
	public boolean shouldIgnoreBlock(BlockPos pos)
	{
		return this.ignoreSet.contains(pos);
	}

}
