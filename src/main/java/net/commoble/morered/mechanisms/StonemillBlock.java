package net.commoble.morered.mechanisms;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.ExtractOnlyGenericItemHandler;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class StonemillBlock extends Block implements EntityBlock
{
	/** Number of progress ticks needed to "mine" a cobblestone */
	public static final int MAX_PROGRESS = 64;
	/** Number of ticks after mining a cobblestone before a new cobblestone forms and "mining" begins **/ 
	public static final int LAVA_TICKS = 30;
	/** Minimum angular velocity to "mine" cobblestone (radians/second) **/
	public static final double MIN_VELOCITY = Math.TAU / 32D;
	
	/**
	 * X -> grates face west/east
	 * Z -> grates face north/south
	 */
	public static final EnumProperty<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	
	/**
	 * ENABLED is true if we have enough fluid
	 */
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	
	/**
	 * At fluid depth of 4 the fluid visually overlaps the holes in the grates
	 */
	public static final int MIN_FLUID_REQUIRED = 4;
	
	public StonemillBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(HORIZONTAL_AXIS, Axis.X)
			.setValue(ENABLED, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.STONEMILL_BLOCK_ENTITY.get().create(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(HORIZONTAL_AXIS, ENABLED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Axis axis = context.getHorizontalDirection().getAxis();
		return this.defaultBlockState()
			.setValue(HORIZONTAL_AXIS, axis);
	}

	@Override
	protected void neighborChanged(BlockState thisState, Level level, BlockPos thisPos, Block neighborBlock, Orientation orientation, boolean isMoving)
	{
		super.neighborChanged(thisState, level, thisPos, neighborBlock, orientation, isMoving);
		level.scheduleTick(thisPos, this, 1);
	}
	
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		super.tick(state, level, pos, rand);
		boolean oldValue = state.getValue(ENABLED);
		boolean newValue = isEnabled(pos, state.getValue(HORIZONTAL_AXIS), level);
		if (oldValue != newValue)
		{
			level.setBlockAndUpdate(pos, state.setValue(ENABLED, newValue));
		}
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult rayTrace)
	{
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GenericBlockEntity be)
		{
			serverPlayer.openMenu(StonemillMenu.serverMenuProvider(be));
		}
		return InteractionResult.SUCCESS;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return level.isClientSide && type == MoreRed.STONEMILL_BLOCK_ENTITY.get() ? null : (BlockEntityTicker<T>) TICKER;
	}

	public static final BlockEntityTicker<GenericBlockEntity> TICKER = StonemillBlock::serverTick;
	public static void serverTick(Level level, BlockPos pos, BlockState state, GenericBlockEntity be)
	{
		// firstly check if we are enabled (have fluid inputs)
		boolean enabled = state.getValue(ENABLED);
		if (!enabled)
			return;
		
		// get data and check generation timestamp
		DataComponentType<StonemillData> stonemillDataComponent = MoreRed.STONEMILL_DATA_COMPONENT.get();
		StonemillData oldData = be.getOrDefault(stonemillDataComponent, StonemillData.ZERO);
		// if we are on tick 30, play the lava cooling effects
		long genTimestamp = oldData.genTimestamp;
		long currentTick = level.getGameTime();
		long ticksSinceGen = currentTick - genTimestamp;
		if (ticksSinceGen == LAVA_TICKS)
		{
			int magicFizzNumber = 1501; // see LevelEventHandler
			level.levelEvent(magicFizzNumber, pos, 0); // LiquidBlock#fizz, plays the lava/water/cobblestone sfx and particles
		}
		
		if (ticksSinceGen <= LAVA_TICKS)
			return;
		// if we're greater than tick 30, check machine progress
		Map<NodeShape, MechanicalState> map = be.getData(MechanicalNodeStates.HOLDER.get());
		double velocity = Math.abs(map.getOrDefault(NodeShape.ofSide(Direction.UP), MechanicalState.ZERO).angularVelocity());
		// if we are not rotating (or rotation is very small), don't do anything else
		// if we are rotating, accumulate progress
		int progressThisTick = (int)(velocity / MIN_VELOCITY);
		if (progressThisTick < 1)
			return;
		int newCurrentProgress = oldData.progress + progressThisTick;
		// then, if progress >= 100%, generate cobblestone and reset timestamp and progress
		if (newCurrentProgress >= MAX_PROGRESS)
		{
			int magicDestroyNumber = 2001; // see LevelEventHandler
			level.levelEvent(magicDestroyNumber, pos, Block.getId(Blocks.COBBLESTONE.defaultBlockState()));
			be.set(stonemillDataComponent, StonemillData.createNew(currentTick));
			ItemContainerContents inventory = getInventory(be);
			ItemStack oldStack = inventory.getSlots() > 0 ? inventory.getStackInSlot(0) : ItemStack.EMPTY;
			ItemStack newStack = oldStack.copy();
			// if cobblestone is full, try to push stack into inventory below
			if (newStack.getCount() == newStack.getMaxStackSize())
			{
				var inventoryBelow = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
				if (inventoryBelow != null)
				{
					newStack = ItemHandlerHelper.insertItemStacked(inventoryBelow, newStack, false);
				}
			}
			// then add a cobblestone to the inventory if possible
			if (newStack.getCount() < newStack.getMaxStackSize())
			{
				if (newStack.isEmpty())
				{
					newStack = new ItemStack(Items.COBBLESTONE);
				}
				else if (newStack.getItem() == Items.COBBLESTONE)
				{
					newStack.grow(1);
				}
			}
			if (oldStack.getCount() != newStack.getCount()
				|| !ItemStack.isSameItem(oldStack, newStack))
			{
				setInventory(be, newStack);
			}
		}
		else
		{
			// otherwise just accumulate progress counter
			be.set(stonemillDataComponent, oldData.withProgress(newCurrentProgress));
		}
	}
	
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving)
	{
		super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
		level.invalidateCapabilities(pos);
	}
	
	public static IItemHandler getItemHandler(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Direction context)
	{
		if (level.getBlockEntity(pos) instanceof GenericBlockEntity be)
		{
			return new ExtractOnlyGenericItemHandler(be, DataComponents.CONTAINER, 1);
		}
		return null;
	}

	public static void preRemoveSideEffects(BlockPos pos, BlockState state, GenericBlockEntity be)
	{
		ItemStack stack = be.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
		if (!stack.isEmpty())
		{
			Containers.dropItemStack(be.getLevel(), pos.getX(), pos.getY(), pos.getZ(), stack);			
		}
	}

	private static boolean isEnabled(BlockPos pos, Axis axis, BlockGetter level)
	{
		Direction[] dirs = axis.getDirections();
		FluidState fluidA = level.getFluidState(pos.relative(dirs[0]));
		FluidState fluidB = level.getFluidState(pos.relative(dirs[1]));
		return ((fluidA.is(Tags.Fluids.WATER) && fluidB.is(Tags.Fluids.LAVA)) || (fluidA.is(Tags.Fluids.LAVA) && fluidB.is(Tags.Fluids.WATER)))
			&& fluidA.getAmount() >= MIN_FLUID_REQUIRED && fluidB.getAmount() >= MIN_FLUID_REQUIRED;
	}
	
	private static ItemContainerContents getInventory(GenericBlockEntity be)
	{
		return be.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
	}
	
	private static void setInventory(GenericBlockEntity be, ItemStack stack)
	{
		be.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack)));
	}
	
	public static record StonemillData(long genTimestamp, int progress)
	{
		public static final Codec<StonemillData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.LONG.fieldOf("gen_timestamp").forGetter(StonemillData::genTimestamp),
				Codec.INT.fieldOf("progress").forGetter(StonemillData::progress)
			).apply(builder, StonemillData::new));
		
		public static final StonemillData ZERO = new StonemillData(0L,0);
		
		public static StonemillData createNew(long genTimestamp)
		{
			return new StonemillData(genTimestamp, 0);
		}
		
		public StonemillData withProgress(int progress)
		{
			return new StonemillData(this.genTimestamp, progress);
		}
	}
}
