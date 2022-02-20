package commoble.morered.wire_post;

import com.google.common.collect.ImmutableSet;
import commoble.morered.TileEntityRegistrar;
import commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Function;

public abstract class AbstractPoweredWirePostBlock extends AbstractPostBlock implements EntityBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final EnumSet<Direction> NO_DIRECTIONS = EnumSet.noneOf(Direction.class);

    protected static final VoxelShape[] POST_SHAPES_DUNSWE = {
            Block.box(6D, 0D, 6D, 10D, 10D, 10D),
            Block.box(6D, 6D, 6D, 10D, 16D, 10D),
            Block.box(6D, 6D, 0D, 10D, 10D, 10D),
            Block.box(6D, 6D, 6D, 10D, 10D, 16D),
            Block.box(0D, 6D, 6D, 10D, 10D, 10D),
            Block.box(6D, 6D, 6D, 16D, 10D, 10D)
    };

    // function that gets the connected directions for a given blockstate
    private final Function<BlockState, EnumSet<Direction>> connectionGetter;

    public AbstractPoweredWirePostBlock(Block.Properties properties,
                                        Function<BlockState, EnumSet<Direction>> connectionGetter) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(POWER, 0));
        this.connectionGetter = connectionGetter;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityRegistrar.REDWIRE_POST.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place
     * logic
     */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        int oldPower = state.getValue(POWER);
        int newPower = this.getNewPower(state, world, pos);
        if (oldPower != newPower) {
            world.setBlock(pos, state.setValue(POWER, newPower), 2);
        }

    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
        int oldPower = state.getValue(POWER);
        int newPower = this.getNewPower(state, world, pos);
        if (oldPower != newPower) {
            world.setBlock(pos, state.setValue(POWER, newPower), 2);
        }

    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState attachmentState = super.getStateForPlacement(context);
        return attachmentState == null
                ? null
                : attachmentState.setValue(POWER, this.getNewPower(attachmentState, context.getLevel(),
                context.getClickedPos()));
    }

    /**
     * Called after a block is placed next to this block
     * <p>
     * Update the provided state given the provided neighbor facing and neighbor
     * state, returning a new state. For example, fences make their connections to
     * the passed in state if possible, and wet concrete powder immediately returns
     * its solidified counterpart. Note that this method should ideally consider
     * only the specific face passed in.
     */
    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world,
                                  BlockPos pos, BlockPos facingPos) {
        return state.setValue(POWER, this.getNewPower(state, world, pos));
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change
     * based on its state.
     *
     * @param state A blockstate of this block
     */
    @Deprecated
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return side != null && this.getConnectableDirections(state).contains(side.getOpposite());
    }

    @Deprecated
    @Override
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getSignal(blockAccess, pos, side);
    }

    @Deprecated
    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos,
                         Direction directionOfThisBlockFromCaller) {
        if (this.getConnectableDirections(blockState).contains(directionOfThisBlockFromCaller.getOpposite())) {
            return blockState.getValue(POWER);
        } else {
            return 0;
        }
    }

    public EnumSet<Direction> getConnectableDirections(BlockState state) {
        return this.connectionGetter.apply(state);
    }

    /**
     * Returns a power level equal to one less than the highest power level among the blocks that this block can
     * connect to
     *
     * @param state The blockstate of this block
     * @param world The world where the blockstate lives
     * @param pos   The position of the blockstate in the world
     * @return The new power level to be used by this block
     */
    public int getNewPower(BlockState state, LevelAccessor world, BlockPos pos) {
        return Math.max(0, Math.max(this.getNeighborPower(state, world, pos), this.getConnectionPower(state, world,
                pos)) - 1);
    }

    /**
     * Returns the highest redstone power level among the neighbors adjacent to this block's redstone-connecting sides
     *
     * @param state The blockstate of this block
     * @param world The world where the blockstate lives
     * @param pos   The position of the state in the world
     * @return The highest redstone power level among the relevant neighbors adjacent to this block
     **/
    public int getNeighborPower(BlockState state, LevelAccessor world, BlockPos pos) {
        if (world instanceof Level) {
            return this.getConnectableDirections(state).stream()
                    .map(direction -> ((Level) world).getSignal(pos.relative(direction), direction))
                    .reduce(0, Math::max);
        } else {
            return 0;
        }
    }

    /**
     * Returns the highest redstone power level among the posts connected to this post
     *
     * @param state The blockstate of this post block
     * @param world The world object the blockstate lives in
     * @param pos   The position of the blockstate in the world
     * @return The highest emitted redstone power level among the posts connected to this post
     **/
    public int getConnectionPower(BlockState state, LevelAccessor world, BlockPos pos) {
        return WorldHelper.getTileEntityAt(WirePostTileEntity.class, world, pos)
                .map(WirePostTileEntity::getRemoteConnections)
                .orElse(ImmutableSet.of())
                .stream()
                .map(world::getBlockState)
                .map(otherState -> otherState.hasProperty(POWER) ? otherState.getValue(POWER) : 0)
                .reduce(0, Math::max);
    }


    @Override
    public void notifyNeighbors(Level world, BlockPos pos, BlockState state) {
        EnumSet<Direction> neighborDirections = this.getConnectableDirections(state);
        if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, pos, world.getBlockState(pos), neighborDirections, false).isCanceled()) {
            for (Direction dir : neighborDirections) {
                BlockPos neighborPos = pos.relative(dir);
                world.neighborChanged(neighborPos, this, pos);
                world.updateNeighborsAtExceptFromFacing(neighborPos, this, dir.getOpposite());
            }
            WorldHelper.getTileEntityAt(WirePostTileEntity.class, world, pos).ifPresent(te -> te.notifyConnections());
        }
    }

}
