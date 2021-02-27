package commoble.morered.wire_post;

import javax.annotation.Nonnull;

import commoble.morered.TileEntityRegistrar;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BundledCableRelayPlateBlock extends AbstractChanneledCablePostBlock
{
	protected static final VoxelShape[] CABLE_PLATE_SHAPES_DUNSWE = {
		Block.makeCuboidShape(0D,0D,0D,16D,4D,16D),
		Block.makeCuboidShape(0D,12D,0D,16D,16D,16D),
		Block.makeCuboidShape(0D,0D,0D,16D,16D,4D),
		Block.makeCuboidShape(0D,0D,12D,16D,16D,16D),
		Block.makeCuboidShape(0D,0D,0D,4D,16D,16D),
		Block.makeCuboidShape(12D,0D,0D,16D,16D,16D)
	};
	
	protected static final VoxelShape[] PLATED_CABLE_POST_SHAPES_DUNSWE = {
		VoxelShapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[0], CABLE_PLATE_SHAPES_DUNSWE[0]), // down
		VoxelShapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[1], CABLE_PLATE_SHAPES_DUNSWE[1]), // up
		VoxelShapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[2], CABLE_PLATE_SHAPES_DUNSWE[2]), // north
		VoxelShapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[3], CABLE_PLATE_SHAPES_DUNSWE[3]), // south
		VoxelShapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[4], CABLE_PLATE_SHAPES_DUNSWE[4]), // west
		VoxelShapes.or(AbstractChanneledCablePostBlock.CABLE_POST_SHAPES_DUNSWE[5], CABLE_PLATE_SHAPES_DUNSWE[5]) // east
	};

	public BundledCableRelayPlateBlock(Properties properties)
	{
		super(properties);
	}
	
	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return TileEntityRegistrar.BUNDLED_CABLE_RELAY_PLATE.get().create();
	}
	
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		// if we're raytracing a wire, ignore the post (the plate can still block the raytrace)
		VoxelShape[] shapeTable = context instanceof WireRayTraceSelectionContext && ((WireRayTraceSelectionContext)context).shouldIgnoreBlock(pos)
			? CABLE_PLATE_SHAPES_DUNSWE
			: PLATED_CABLE_POST_SHAPES_DUNSWE;
		return shapeTable[state.hasProperty(DIRECTION_OF_ATTACHMENT) ? state.get(DIRECTION_OF_ATTACHMENT).ordinal() : 0];
	}
	
	public boolean canConnectToAdjacentCable(@Nonnull IBlockReader world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire)
	{
		Direction postAttachmentDir = thisState.get(AbstractPostBlock.DIRECTION_OF_ATTACHMENT);
		return directionToWire != postAttachmentDir && directionToWire != postAttachmentDir.getOpposite() && postAttachmentDir == wireFace;
	}
	/**
	 * Called by ItemBlocks after a block is set in the world, to allow post-place
	 * logic
	 */
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		this.updatePower(world, pos);
	}
}
