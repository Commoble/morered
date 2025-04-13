package net.commoble.morered.mechanisms;

import java.util.Map;
import java.util.function.Supplier;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

public class ClutchBlock extends Block implements EntityBlock
{
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
	
	private static final Map<Direction,VoxelShape> UNEXTENDED_SHAPES = Shapes.rotateAll(Block.box(0D,0D,5D,16D,16D,16D));
	private static final Map<Direction,VoxelShape> EXTENDED_SHAPES = Shapes.rotateAll(
		Shapes.or(
			Block.box(0D,0D,8D,16D,16D,16D),
			Block.box(6D,6D,3D,10D,10D,8D),
			GearBlock.SHAPES.get(Direction.NORTH)));

	private final Supplier<ItemStack> gearItem;
	
	public ClutchBlock(Properties props, Supplier<ItemStack> gearItem)
	{
		super(props);
		this.gearItem = gearItem;
		this.registerDefaultState(this.defaultBlockState()
			.setValue(FACING, Direction.NORTH)
			.setValue(EXTENDED, false));
	}
	
	public ItemStack getGearItem()
	{
		return this.gearItem.get();
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(FACING, EXTENDED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().clutchBlockEntity.get().create(pos,state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return state.getValue(EXTENDED)
			? EXTENDED_SHAPES.get(state.getValue(FACING))
			: UNEXTENDED_SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@Deprecated
	protected BlockState mirror(BlockState state, Mirror rotation)
	{
		return state.rotate(rotation.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving)
	{
		boolean hasSignal = hasNeighborSignal(level, pos, state.getValue(FACING));
		boolean hadSignal = state.getValue(EXTENDED);
		if (hasSignal && !hadSignal)
		{
			level.setBlockAndUpdate(pos, state.setValue(EXTENDED, true));
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.25F + 0.6F);
		}
		else if (!hasSignal && hadSignal)
		{
			level.setBlockAndUpdate(pos, state.setValue(EXTENDED, false));
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
		}
	}
	
	// same as piston
	private boolean hasNeighborSignal(SignalGetter level, BlockPos thisPos, Direction clutchFacing)
	{
		for (Direction directionToNeighbor : Direction.values())
		{
			if (directionToNeighbor != clutchFacing && level.hasSignal(thisPos.relative(directionToNeighbor), directionToNeighbor))
			{
				return true;
			}
		}

		if (level.hasSignal(thisPos, Direction.DOWN))
		{
			return true;
		}
		else
		{
			BlockPos abovePos = thisPos.above();

			for (Direction directionToNeighbor : Direction.values())
			{
				if (directionToNeighbor != Direction.DOWN && level.hasSignal(abovePos.relative(directionToNeighbor), directionToNeighbor))
				{
					return true;
				}
			}

			return false;
		}
	}

	// only block light if facing vertically
	@Override
	protected boolean propagatesSkylightDown(BlockState state)
	{
		return state.getValue(FACING).getAxis() != Direction.Axis.Y;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
	{
		if (stack.is(Tags.Items.TOOLS_WRENCH))
		{
			level.playSound(player, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS,
				0.1F + level.random.nextFloat()*0.1F,
				0.7F + level.random.nextFloat()*0.1F);
			level.setBlock(pos, state.cycle(FACING), UPDATE_ALL);
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}
}
