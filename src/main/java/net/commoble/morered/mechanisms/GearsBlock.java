package net.commoble.morered.mechanisms;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.OctahedralGroup;

import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.util.EightGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;

// TODO make flammable unless all component gears are not flammable
public class GearsBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, FaceSegmentBlock
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<OctahedralGroup> TRANSFORM = EightGroup.TRANSFORM;
    
	public static final Map<BlockState, VoxelShape> SHAPE_CACHE = new HashMap<>();
	public static final Map<Integer, VoxelShape> SHAPES_BY_BITFLAG = new HashMap<>();
	
	public static Pair<Direction,ItemStack> getNearestExistingGear(BlockPos pos, BlockState state, GenericBlockEntity be, Vec3 worldHitVec)
	{
		double nearestDistSquared = Double.MAX_VALUE;
		Pair<Direction,ItemStack> nearest = Pair.of(Direction.NORTH, ItemStack.EMPTY);
		Vec3 localHitVec = worldHitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
		var items = be.get(MoreRed.GEARS_DATA_COMPONENT.get());
		if (items != null)
		{
			for (var entry : FaceSegmentBlock.HITVECS.entrySet())
			{
				Direction side = entry.getKey();
				if (state.getValue(FaceSegmentBlock.getProperty(side)))
				{
					ItemStack gear = items.get(entry.getKey());
					if (!gear.isEmpty())
					{
						Vec3 sideCenter = entry.getValue();
						double distSquared = localHitVec.distanceToSqr(sideCenter);
						if (distSquared < nearestDistSquared)
						{
							nearest = Pair.of(side,gear);
							nearestDistSquared = distSquared;
						}
					}
				}
			}
		}
		return nearest;
	}
	
	public GearsBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(DOWN, false)
			.setValue(UP, false)
			.setValue(NORTH, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false)
			.setValue(EAST, false)
			.setValue(WATERLOGGED, false)
			.setValue(TRANSFORM, OctahedralGroup.IDENTITY));
	}
	
	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DOWN,UP,NORTH,SOUTH,WEST,EAST, WATERLOGGED, TRANSFORM);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.GEARS_BLOCK_ENTITY.get().create(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPE_CACHE.computeIfAbsent(state, GearsBlock::makeShapeFromState);
	}
	
	protected static VoxelShape makeShapeFromState(BlockState state)
	{
		int bitflag = 0;
		for (int i=0; i<6; i++)
		{
			Direction dir = Direction.values()[i];
			if (state.getValue(FaceSegmentBlock.getProperty(dir)))
			{
				bitflag |= (1 << i);
			}
		}
		return SHAPES_BY_BITFLAG.computeIfAbsent(bitflag, GearsBlock::makeShape);
	}
	
	protected static boolean hasNoGears(BlockState state)
	{
		return !state.getValue(DOWN)
			&& !state.getValue(UP)
			&& !state.getValue(NORTH)
			&& !state.getValue(SOUTH)
			&& !state.getValue(WEST)
			&& !state.getValue(EAST);
	}
	
	public static VoxelShape makeShape(int bitflag)
	{
		VoxelShape shape = Shapes.empty();
		for (int bit=0; bit<6; bit++)
		{
			if ((bitflag & (1 << bit)) > 0)
			{
				Direction dir = Direction.values()[bit];
				VoxelShape baseShape = GearBlock.SHAPES.get(dir);
				shape = Shapes.or(shape, baseShape);
			}
		}
		return shape;
	}

	@Override
	protected BlockState updateShape(
		BlockState thisState,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos thisPos,
		Direction directionToNeighbor,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random)
	{
		if (thisState.getValue(WATERLOGGED))
		{
			ticks.scheduleTick(thisPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		// clear any empty gears that get left around
		return hasNoGears(thisState)
			? Blocks.AIR.defaultBlockState()
			: thisState;
	}
	
	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		BlockState result = state;
		for (int i=0; i<4; i++) // rotations only rotated about the y-axis, we only need to rotated the horizontal faces
		{
			Direction dir = Direction.from2DDataValue(i);
			Direction newDir = rot.rotate(dir);
			result = result.setValue(FaceSegmentBlock.getProperty(newDir), state.getValue(FaceSegmentBlock.getProperty(dir)));
		}
		result = EightGroup.rotate(result, rot);
		return result;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		BlockState result = state;
		for (int i=0; i<4; i++) // only horizontal sides get mirrored
		{
			Direction dir = Direction.from2DDataValue(i);
			Direction newDir = mirrorIn.mirror(dir);
			result = result.setValue(FaceSegmentBlock.getProperty(newDir), state.getValue(FaceSegmentBlock.getProperty(dir)));
		}
		result = EightGroup.mirror(result, mirrorIn);
		return result;
	}
	
	@Override
	protected FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	// but, with pickblock, we should try to pick the best gear the player is selecting
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player)
	{
		// try to get the nearest gear to the pick location if possible
		double range = player.blockInteractionRange();
		if (level.getBlockEntity(pos) instanceof GenericBlockEntity be)
		{
			HitResult result = player.pick(range, 0F, false);
			if (result.getType() != HitResult.Type.MISS)
			{
				ItemStack gear = getNearestExistingGear(pos, state, be, result.getLocation()).getSecond();
				if (!gear.isEmpty())
				{
					return gear;
				}
			}
			
			var items = be.get(MoreRed.GEARS_DATA_COMPONENT.get());
			if (items != null)
			{
				// if the raytrace failed, try to estimate from nearest direction
				for (Direction lookDirection : Direction.orderedByNearest(player))
				{
					Direction gearDirection = lookDirection.getOpposite();
					ItemStack gear = items.get(gearDirection);
					if (state.getValue(FaceSegmentBlock.getProperty(gearDirection)) && !gear.isEmpty())
					{
						return gear;
					}
				}
			}
		}
		
		// fallback, default to oak gear (we have no item at all for this block, can't just store a blockentity copy in it)
		return new ItemStack(MoreRed.GEAR_BLOCKS.get("oak").get());
	}

	// called after a block is placed into an empty position and blockentity data has been transferred from itemstack
	// also called after a gear is added to an existing gear block
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack)
	{
		// set blockentity data
		if (level.getBlockEntity(pos) instanceof GenericBlockEntity be)
		{
			var gearsComponent = MoreRed.GEARS_DATA_COMPONENT.get();
			Map<Direction, ItemStack> oldData = Objects.requireNonNullElseGet(be.get(gearsComponent), HashMap::new);
			if (oldData != null)
			{
				boolean doUpdate = false;
				Map<Direction, ItemStack> newData = new HashMap<>(oldData);
				for (Direction dir : Direction.values())
				{
					if (state.getValue(FaceSegmentBlock.getProperty(dir)) && !newData.containsKey(dir))
					{
						newData.put(dir, stack.copyWithCount(1));
						doUpdate = true;
					}
				}
				if (doUpdate)
				{
					be.set(gearsComponent, newData);
				}
			}
		}
		super.setPlacedBy(level, pos, state, entity, stack);
	}
	
	

	@Override
	protected void onPlace(BlockState newState, Level level, BlockPos pos, BlockState oldState, boolean isMoving)
	{
		if (level.getBlockEntity(pos) instanceof GenericBlockEntity be)
		{
			var items = be.get(MoreRed.GEARS_DATA_COMPONENT.get());
			if (items != null)
			{
				// if we are altering which gear states exist, clean out the datacomponent
				if (newState.getBlock() == oldState.getBlock())
				{
					Map<Direction,ItemStack> newDataComponent = new HashMap<>();
					for (Direction dir : Direction.values())
					{
						@Nullable ItemStack stack = items.get(dir);
						if (stack != null)
						{
							if (newState.getValue(FaceSegmentBlock.getProperty(dir)))
							{
								newDataComponent.put(dir, stack);
							}
						}
					}
					be.set(MoreRed.GEARS_DATA_COMPONENT.get(), newDataComponent);
				}
			}
			if (this.isEmptyGearsBlock(newState))
			{
				// if gears is empty, remove it next tick
				// (can't destroy in onPlace)
				level.scheduleTick(pos, this, 1);
			}
		}
		super.onPlace(newState, level, pos, oldState, isMoving);
	}
	
	public boolean isEmptyGearsBlock(BlockState state)
	{
		return state == this.defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
	}
	
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		super.tick(state, level, pos, random);
		if (this.isEmptyGearsBlock(state)) {
			level.removeBlock(pos,  false);
		}
	}

	public static Map<Direction, ItemStack> normalizeGears(BlockState state, Map<Direction,ItemStack> oldValues)
	{
		OctahedralGroup group = state.getValue(TRANSFORM);
		if (group == OctahedralGroup.IDENTITY)
			return oldValues;
		Map<Direction,ItemStack> newValues = new HashMap<>();
		for (var entry : newValues.entrySet())
		{
			Direction dir = entry.getKey();
			Direction normalizedDir = group.inverse().rotate(dir);
			newValues.put(normalizedDir, entry.getValue());
		}
		return newValues;
	}
	
	public static Map<Direction, ItemStack> denormalizeGears(BlockState state, Map<Direction,ItemStack> oldValues)
	{
		OctahedralGroup group = state.getValue(TRANSFORM);
		if (group == OctahedralGroup.IDENTITY)
			return oldValues;
		Map<Direction,ItemStack> newValues = new HashMap<>();
		for (var entry : newValues.entrySet())
		{
			Direction dir = entry.getKey();
			Direction normalizedDir = group.rotate(dir);
			newValues.put(normalizedDir, entry.getValue());
		}
		return newValues;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.INVISIBLE;
	}
	
	public void handleLeftClickBlock(LeftClickBlock event, Level level, BlockPos pos, BlockState state)
	{
		Player player = event.getEntity();
		if (!(player.isCreative() || event.getAction() == LeftClickBlock.Action.STOP))
			return;
		
		if (!(level instanceof ServerLevel serverLevel))
			return;
		
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		
		if (!(level.getBlockEntity(pos) instanceof GenericBlockEntity be))
			return;
		
		HitResult hit = player.pick(player.blockInteractionRange(), 0F, false);
		if (hit.getType() == HitResult.Type.MISS)
			return;
		
		// override event from here
		event.setCanceled(true);
		
		// we still have to redo a few of the existing checks
		if (!serverPlayer.canInteractWithBlock(pos, 1.0)
			|| (pos.getY() >= serverLevel.getMaxY() || pos.getY() < serverLevel.getMinY())
			|| !serverLevel.mayInteract(serverPlayer, pos) // checks spawn protection and world border
			|| CommonHooks.fireBlockBreak(serverLevel, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos, state).isCanceled()
			|| serverPlayer.blockActionRestricted(serverLevel, pos, serverPlayer.gameMode.getGameModeForPlayer()))
		{
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			return;
		}
		Direction destroySide = getNearestExistingGear(pos, state, be, hit.getLocation()).getFirst();
		
		if (serverPlayer.isCreative())
		{
			this.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, false);
			return;
		}
		if (!state.canHarvestBlock(serverLevel, pos, serverPlayer))
		{
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			return;
		}
		this.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, true);
	}

	@Override
	public Map<Direction, VoxelShape> getRaytraceBackboards()
	{
		return GearBlock.SHAPES;
	}
}
