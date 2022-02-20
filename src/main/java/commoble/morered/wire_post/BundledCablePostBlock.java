package commoble.morered.wire_post;

import commoble.morered.TileEntityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BundledCablePostBlock extends AbstractChanneledCablePostBlock implements EntityBlock {

    public BundledCablePostBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        // if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
        return context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext) context).shouldIgnoreBlock(pos)
                ? Shapes.empty()
                :
                AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[state.hasProperty(DIRECTION_OF_ATTACHMENT) ?
                        state.getValue(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityRegistrar.BUNDLED_CABLE_POST.get().create(pos, state);
    }

}
