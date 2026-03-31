package net.commoble.morered.transportation;

import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class LoaderBlock extends Block
{
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;

	public LoaderBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult rayTrace)
	{
		if (player instanceof ServerPlayer serverPlayer)
		{
			serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, theServerPlayer) ->
				new LoaderMenu(id, inventory, pos), Component.translatable(this.getDescriptionId()))
			);
		}

		return InteractionResult.SUCCESS;
	}

	// returns the portion of the itemstack that wasn't inserted
	public ItemStack insertItem(ItemStack stack, Level world, BlockPos pos, BlockState state)
	{
		// check if it can insert the item
		Direction outputDir = state.getValue(FACING);
		BlockPos outputPos = pos.relative(outputDir);
		ResourceHandler<ItemResource> outputHandler = world.getCapability(Capabilities.Item.BLOCK, outputPos, outputDir.getOpposite());
		ItemStack remaining = outputHandler == null
			? stack.copy()
			: WorldHelper.insertItemStackedImmediate(outputHandler, stack);

		if (remaining.getCount() > 0) // we have remaining items
		{
			WorldHelper.ejectItemstack(world, pos, outputDir, remaining);
	        world.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.3F, world.getRandom().nextFloat() * 0.25F + 2F);
			return ItemStack.EMPTY;
		}
		else	// item was accepted fully
		{
	        world.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.3F, world.getRandom().nextFloat() * 0.25F + 1F);
			return ItemStack.EMPTY;
		}
	}

	//// facing and blockstate boilerplate

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return this.defaultBlockState().setValue(FACING, WorldHelper.getBlockFacingForPlacement(context));
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	@Deprecated
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
	{
		if (stack.is(Tags.Items.TOOLS_WRENCH))
		{
			level.playSound(player, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS,
				0.1F + level.getRandom().nextFloat()*0.1F,
				0.7F + level.getRandom().nextFloat()*0.1F);
			level.setBlock(pos, state.cycle(FACING), UPDATE_ALL);
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}
}
