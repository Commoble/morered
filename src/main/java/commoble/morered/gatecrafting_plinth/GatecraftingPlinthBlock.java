package commoble.morered.gatecrafting_plinth;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class GatecraftingPlinthBlock extends Block {
    public static final VoxelShape SHAPE = Shapes.or(
            // plate
            Block.box(0, 14, 0, 16, 16, 16),
            // blaze rod
            Block.box(6, 0, 6, 10, 14, 10),
            // legs
            Block.box(0, 0, 0, 4, 14, 4),
            Block.box(0, 0, 12, 4, 14, 16),
            Block.box(12, 0, 0, 16, 14, 4),
            Block.box(12, 0, 12, 16, 14, 16));


    public GatecraftingPlinthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult rayTrace) {
        if (!world.isClientSide) {
//            BlockEntity b = world.getBlockEntity(pos);
            if (player instanceof ServerPlayer) {
                MenuProvider provider = GatecraftingContainer.getServerContainerProvider(pos);
                Component name = new TranslatableComponent(this.getDescriptionId());
                MenuProvider s = new SimpleMenuProvider(provider, name);
                NetworkHooks.openGui((ServerPlayer) player, s, pos);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
