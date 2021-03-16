package commoble.morered.wire_post;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.EntitySelectionContext;

/** We have nullable entities in RayTraceContext, but we still need the ignore blocks for collision checking **/
public class WireRayTraceSelectionContext extends EntitySelectionContext
{
	private final Set<BlockPos> ignoreSet;
	
	public WireRayTraceSelectionContext(BlockPos start, BlockPos end)
	{
		super(false, -Double.MAX_VALUE, Items.AIR, fluid -> false); // same as EntitySelectionContext.DUMMY
		this.ignoreSet = ImmutableSet.of(start.immutable(), end.immutable());
	}
	
	public boolean shouldIgnoreBlock(BlockPos pos)
	{
		return this.ignoreSet.contains(pos);
	}

}
