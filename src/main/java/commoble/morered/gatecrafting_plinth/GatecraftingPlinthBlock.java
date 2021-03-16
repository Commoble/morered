package commoble.morered.gatecrafting_plinth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import net.minecraft.block.AbstractBlock.Properties;

public class GatecraftingPlinthBlock extends Block
{
	public static final VoxelShape SHAPE = VoxelShapes.or(
		// plate
		Block.box(0, 14, 0, 16, 16, 16),
		// blaze rod
		Block.box(6, 0, 6, 10, 14, 10),
		// legs
		Block.box(0, 0, 0, 4, 14, 4),
		Block.box(0, 0, 12, 4, 14, 16),
		Block.box(12, 0, 0, 16, 14, 4),
		Block.box(12, 0, 12, 16, 14, 16));
		

	public GatecraftingPlinthBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTrace)
	{
		if (player instanceof ServerPlayerEntity)
		{
			IContainerProvider provider = GatecraftingContainer.getServerContainerProvider(pos);
			ITextComponent name = new TranslationTextComponent(this.getDescriptionId());
			INamedContainerProvider namedProvider = new SimpleNamedContainerProvider(provider, name);
			NetworkHooks.openGui((ServerPlayerEntity)player, namedProvider);
		}

		return ActionResultType.SUCCESS;
	}
	
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		return SHAPE;
	}
}
