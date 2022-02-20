package commoble.morered.wire_post;

import java.util.EnumSet;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WirePostBlock extends AbstractPoweredWirePostBlock {

    public WirePostBlock(Properties properties, Function<BlockState, EnumSet<Direction>> connectionGetter) {
        super(properties, connectionGetter);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        // if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
        return context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext) context).shouldIgnoreBlock(pos)
                ? Shapes.empty()
                : AbstractPoweredWirePostBlock.POST_SHAPES_DUNSWE[state.hasProperty(DIRECTION_OF_ATTACHMENT) ?
                state.getValue(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
    }

    public static EnumSet<Direction> getRedstoneConnectionDirections(BlockState state) {
        return state.hasProperty(DIRECTION_OF_ATTACHMENT)
                ? EnumSet.of(state.getValue(DIRECTION_OF_ATTACHMENT))
                : AbstractPoweredWirePostBlock.NO_DIRECTIONS;
    }

}
