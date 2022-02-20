package commoble.morered.bitwise_logic;

import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.moreredapi.ChanneledPowerSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.common.Tags;

public abstract class BitwiseLogicPlateBlock extends PlateBlock implements EntityBlock {
    public static final int TICK_DELAY = 1;
    public static final VoxelShape[] DOUBLE_PLATE_SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(0, 12, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 16, 4),
            Block.box(0, 0, 12, 16, 16, 16),
            Block.box(0, 0, 0, 4, 16, 16),
            Block.box(12, 0, 0, 16, 16, 16)};
    public static final ChanneledPowerSupplier NO_POWER_SUPPLIER = (world, pos, state, dir, channel) -> 0;

    protected abstract void updatePower(Level world, BlockPos thisPos, BlockState thisState);

    public abstract boolean canConnectToAdjacentCable(@Nonnull BlockGetter world, @Nonnull BlockPos thisPos,
                                                      @Nonnull BlockState thisState, @Nonnull BlockPos wirePos,
                                                      @Nonnull BlockState wireState, @Nonnull Direction wireFace,
                                                      @Nonnull Direction directionToWire);

    public BitwiseLogicPlateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityRegistrar.BITWISE_LOGIC_PLATE.get().create(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if (state.hasProperty(ATTACHMENT_DIRECTION)) {
            return DOUBLE_PLATE_SHAPES_BY_DIRECTION[state.getValue(ATTACHMENT_DIRECTION).ordinal()];
        } else {
            return DOUBLE_PLATE_SHAPES_BY_DIRECTION[0];
        }
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place
     * logic
     */
    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        worldIn.scheduleTick(pos, this, 1);
    }

    // forge hook, signals that a neighboring block's TE data or comparator data updated
    @Override
    public void onNeighborChange(BlockState thisState, LevelReader world, BlockPos thisPos, BlockPos neighborPos) {
        super.onNeighborChange(thisState, world, thisPos, neighborPos);
        if (world instanceof Level) {
            ((Level) world).scheduleTick(thisPos, this, TICK_DELAY, TickPriority.HIGH);
        }

    }

    @Override
    @Deprecated
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        // if this is just a blockstate change, make sure we tell the TE to invalidate and reset its capability
        if (state.getBlock() == newState.getBlock()) {
            BlockEntity te = worldIn.getBlockEntity(pos);
            if (te instanceof ChanneledPowerStorageTileEntity) {
                ((ChanneledPowerStorageTileEntity) te).resetCapabilities();
            }
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public void tick(BlockState oldBlockState, ServerLevel world, BlockPos pos, Random rand) {
        this.updatePower(world, pos, oldBlockState);
    }

    @Override
    @Deprecated
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn
            , BlockHitResult hit) {
        boolean isPlayerHoldingStick = Tags.Items.RODS_WOODEN.contains(player.getItemInHand(handIn).getItem());

        // rotate the block when the player pokes it with a stick
        if (isPlayerHoldingStick && !worldIn.isClientSide) {
            int newRotation = (state.getValue(ROTATION) + 1) % 4;
            BlockState newState = state.setValue(ROTATION, newRotation);
            worldIn.setBlockAndUpdate(pos, newState);
        }

        return isPlayerHoldingStick ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

}
