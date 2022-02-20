package commoble.morered.wire_post;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/**
 * We have nullable entities in RayTraceContext, but we still need the ignore blocks for collision checking
 **/
public class WireRayTraceSelectionContext extends EntityCollisionContext {
    private final Set<BlockPos> ignoreSet;

    public WireRayTraceSelectionContext(BlockPos start, BlockPos end) {
        super(false, -Double.MAX_VALUE, ItemStack.EMPTY, fluid -> false, null); // same as EntitySelectionContext.DUMMY
        this.ignoreSet = ImmutableSet.of(start.immutable(), end.immutable());
    }

    public boolean shouldIgnoreBlock(BlockPos pos) {
        return this.ignoreSet.contains(pos);
    }

}
