package commoble.morered.wire_post;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import commoble.morered.TileEntityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BundledCableRelayPlateBlock extends AbstractChanneledCablePostBlock implements EntityBlock {
    protected static final VoxelShape[] CABLE_PLATE_SHAPES_DUNSWE = {
            Block.box(0D, 0D, 0D, 16D, 4D, 16D),
            Block.box(0D, 12D, 0D, 16D, 16D, 16D),
            Block.box(0D, 0D, 0D, 16D, 16D, 4D),
            Block.box(0D, 0D, 12D, 16D, 16D, 16D),
            Block.box(0D, 0D, 0D, 4D, 16D, 16D),
            Block.box(12D, 0D, 0D, 16D, 16D, 16D)
    };

    protected static final VoxelShape[] PLATED_CABLE_POST_SHAPES_DUNSWE = {
            Shapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[0], CABLE_PLATE_SHAPES_DUNSWE[0]), //
            // down
            Shapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[1], CABLE_PLATE_SHAPES_DUNSWE[1]), // up
            Shapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[2], CABLE_PLATE_SHAPES_DUNSWE[2]), //
            // north
            Shapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[3], CABLE_PLATE_SHAPES_DUNSWE[3]), //
            // south
            Shapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[4], CABLE_PLATE_SHAPES_DUNSWE[4]), //
            // west
            Shapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[5], CABLE_PLATE_SHAPES_DUNSWE[5]) // east
    };

    public BundledCableRelayPlateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityRegistrar.BUNDLED_CABLE_RELAY_PLATE.get().create(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        // if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
        VoxelShape[] shapeTable =
                context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext) context).shouldIgnoreBlock(pos)
                        ? CABLE_PLATE_SHAPES_DUNSWE
                        : PLATED_CABLE_POST_SHAPES_DUNSWE;
        return shapeTable[state.hasProperty(DIRECTION_OF_ATTACHMENT) ?
                state.getValue(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
    }

    public boolean canConnectToAdjacentCable(@Nonnull BlockGetter world, @Nonnull BlockPos thisPos,
                                             @Nonnull BlockState thisState, @Nonnull BlockPos wirePos,
                                             @Nonnull BlockState wireState, @Nonnull Direction wireFace,
                                             @Nonnull Direction directionToWire) {
        Direction postAttachmentDir = thisState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT);
        return directionToWire != postAttachmentDir && directionToWire != postAttachmentDir.getOpposite() && postAttachmentDir == wireFace;
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place
     * logic
     */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        this.updatePower(world, pos);
    }

}
