package net.commoble.morered.transportation;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FilterBlock extends AbstractFilterBlock implements EntityBlock
{
	public static final VoxelShape[] SHAPES = makeShapes();

	public FilterBlock(Properties properties)
	{
		super(properties);
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (player instanceof ServerPlayer serverPlayer)
		{
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof FilterBlockEntity filter)
			{
				serverPlayer.openMenu(new SimpleMenuProvider(FilterMenu.createServerMenuConstructor(filter), Component.translatable(this.getDescriptionId())));
			}
		}
		
		return InteractionResult.SUCCESS;
	}
	
	// shapes
	
	public static VoxelShape[] makeShapes()
	{
		VoxelShape[] shapes = new VoxelShape[6];
		
		for (int face=0; face<6; face++)
		{
			boolean DOWN = face == 0;
			boolean UP = face == 1;
			boolean NORTH = face == 2;
			boolean SOUTH = face == 3;
			boolean WEST = face == 4;
			boolean EAST = face == 5;
			
			// plate shape
			double x_min = WEST ? 14D : 0D;
			double x_max = EAST ? 2D : 16D;
			double y_min = DOWN ? 14D : 0D;
			double y_max = UP ? 2D : 16D;
			double z_min = NORTH ? 14D : 0D;
			double z_max = SOUTH ? 2D : 16D;
			
			VoxelShape plate = Block.box(x_min, y_min, z_min, x_max, y_max, z_max);
			
			// vertical crossbars
			x_min = WEST ? 0D : EAST ? 0D : 6D;
			x_max = WEST ? 16D : EAST ? 16D : 10D;
			y_min = 0D;
			y_max = 16D;
			z_min = WEST ? 6D : EAST ? 6D : 0D;
			z_max = WEST ? 10D : EAST ? 10D : 16D;
			
			VoxelShape vertical = Block.box(x_min, y_min, z_min, x_max, y_max, z_max);
			
			// horizontal crossbars
			x_min = 0D;
			x_max = 16D;
			y_min = UP ? 0D : DOWN ? 0D : 6D;
			y_max = UP ? 16D : DOWN ? 16D : 10D;
			z_min = UP ? 6D : DOWN ? 6D : 0D;
			z_max = UP ? 10D : DOWN ? 10D : 16D;
			
			VoxelShape horizontal = Block.box(x_min, y_min, z_min, x_max, y_max, z_max);
			
			shapes[face] = Shapes.or(plate, vertical, horizontal);
		}
		
		return shapes;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES[this.getShapeIndex(state)];
	}
	
	public int getShapeIndex(BlockState state)
	{
		return state.getValue(FACING).ordinal();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.FILTER_BLOCK_ENTITY.get().create(pos, state);
	}
}
