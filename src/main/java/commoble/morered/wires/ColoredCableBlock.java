package commoble.morered.wires;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

import com.google.common.cache.LoadingCache;

import commoble.morered.MoreRed;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.ExpandedPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered.api.internal.DefaultWireProperties;
import commoble.morered.api.internal.WireVoxelHelpers;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ColoredCableBlock extends PoweredWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = WireVoxelHelpers.makeNodeShapes(2, 3);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = WireVoxelHelpers.makeRaytraceBackboards(3);
	public static final VoxelShape[] LINE_SHAPES = WireVoxelHelpers.makeLineShapes(2, 3);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);
	
	private final DyeColor color; public DyeColor getDyeColor() { return this.color; }

	public ColoredCableBlock(Properties properties, DyeColor color)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false);
		this.color = color;
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().coloredNetworkCableBeType.get().create(pos, state);
	}

	public boolean canConnectToAdjacentWireOrCable(BlockGetter world, BlockPos thisPos,
		BlockState thisState, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire)
	{
		Block wireBlock = wireState.getBlock();
		return wireBlock instanceof ColoredCableBlock && ((ColoredCableBlock)wireBlock).getDyeColor() != this.getDyeColor()
			? false
			: AbstractWireBlock.canWireConnectToAdjacentWireOrCable(world, thisPos, thisState, wirePos, wireState, wireFace, directionToWire);
	}

	@Override
	protected boolean canAdjacentBlockConnectToFace(BlockGetter world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		// do this check first so we don't check the same behaviour twice for colored cable blocks
		return neighborBlock instanceof ColoredCableBlock
			? ((ColoredCableBlock)neighborBlock).canConnectToAdjacentWireOrCable(world, neighborPos, neighborState, thisPos, thisState, attachmentDirection, directionToWire)
			: MoreRedAPI.getWireConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultWireConnector())
				.canConnectToAdjacentWire(world, neighborPos, neighborState, thisPos, thisState, attachmentDirection, directionToWire)
				||
				MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
				.canConnectToAdjacentWire(world, neighborPos, neighborState, thisPos, thisState, attachmentDirection, directionToWire);
	}
	
	// invoked when an adjacent TE is marked dirty or an adjacent block updates its comparator output
	@Override
	public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor)
	{
		// when would we need to update power here?
		// firstly, if the entire blockstate changed, we'll be updating the power elsewhere anyway
		// so we only need to do extra things here if blockstate (and therefore the TE) remain the same,
		// but the power input from the TE's capability changed
		// we can assume that the *existence* of the neighbor TE and its capability itself won't change here
		// so either A) the neighbor TE did not have the capability and will continue to not have it (or there is no TE there)
		// in which case we don't need to do anything 
		// or B) the neighbor TE does have the capability, in which case the power MAY have changed, in which case we should check power
		
		if (!(world instanceof Level level))
			return;
		
		Direction directionFromNeighbor = DirectionHelper.getDirectionToNeighborPos(neighbor, pos);
		if (directionFromNeighbor == null)
			return;
		
		if (level.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, neighbor, directionFromNeighbor) != null)
		{
			this.updatePowerAfterBlockUpdate(level, pos, state);
		}
	}
	
	@Override
	protected void updatePowerAfterBlockUpdate(Level world, BlockPos wirePos, BlockState wireState)
	{
		BlockEntity te = world.getBlockEntity(wirePos);
		if (!(te instanceof WireBlockEntity))
			return; // if there's no TE then we can't make any updates
		WireBlockEntity wire = (WireBlockEntity)te;
		
		Map<Block,WireConnector> wireConnectors = MoreRedAPI.getWireConnectabilityRegistry();
		Map<Block,ExpandedPowerSupplier> expandedPowerSuppliers = MoreRedAPI.getExpandedPowerRegistry();
		WireConnector defaultWireConnector = MoreRedAPI.getDefaultWireConnector();
		ExpandedPowerSupplier defaultPowerSupplier = MoreRedAPI.getDefaultExpandedPowerSupplier();
		Map<Block,WireConnector> cableConnectors = MoreRedAPI.getCableConnectabilityRegistry();
		WireConnector defaultCableConnector = MoreRedAPI.getDefaultCableConnector();
		Map<Direction,ChanneledPowerSupplier> neighborPowerSuppliers = new EnumMap<>(Direction.class);
		ChanneledPowerSupplier noPower = DefaultWireProperties.NO_POWER_SUPPLIER;
		Function<BlockPos, Function<Direction, ChanneledPowerSupplier>> neighborPowerFinder = neighborPos -> directionToNeighbor ->
		{
			ChanneledPowerSupplier p = world.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, neighborPos, directionToNeighbor.getOpposite());
			return p == null ? noPower : p;
		};
		int channel = this.getDyeColor().ordinal();
		
		BlockPos.MutableBlockPos mutaPos = wirePos.mutable();
		BlockState[] neighborStates = new BlockState[6];
		
		boolean anyPowerUpdated = false;
		
		// set of faces needing updates
		// ONLY faces of attached wires are to be added to this set (check the state before adding)
		EnumSet<Direction> facesNeedingUpdates = EnumSet.noneOf(Direction.class);
//		Set<BlockPos> diagonalNeighborsToUpdate = new HashSet<>();
		boolean attachedFaceStates[] = new boolean[6];
		for (int attachmentSide=0; attachmentSide<6; attachmentSide++)
		{
			if (wireState.getValue(INTERIOR_FACES[attachmentSide]))
			{
				attachedFaceStates[attachmentSide] = true;
				facesNeedingUpdates.add(Direction.from3DDataValue(attachmentSide));
			}
			else
			{
				// wire is not attached on this face, ensure power is 0
				if (wire.setPower(attachmentSide, 0))
				{	// if we lost power here, do neighbor updates later
					anyPowerUpdated = true;
				}
			}
		}
		
		int iteration = 0;
		while (!facesNeedingUpdates.isEmpty())
		{
			int attachmentSide = (iteration++) % 6;
			Direction attachmentDirection = Direction.from3DDataValue(attachmentSide);
			
			// if the set of remaining faces doesn't contain the face, skip the rest of the iteration
			if (!facesNeedingUpdates.remove(attachmentDirection)) 
				continue;
			
			// we know there's a wire attached to the face because we checked the state before adding
			int power = 0;
			// always get power from attached faces, those are full cubes and may supply conducted power
			mutaPos.setWithOffset(wirePos, attachmentDirection);
			BlockState attachedNeighborState = neighborStates[attachmentSide];
			if (attachedNeighborState == null)
			{
				attachedNeighborState = world.getBlockState(mutaPos);
				neighborStates[attachmentSide] = attachedNeighborState;
			}
			ChanneledPowerSupplier attachedNeighborPowerSupplier;
			attachedNeighborPowerSupplier = neighborPowerSuppliers.get(attachmentDirection);
			if (attachedNeighborPowerSupplier == null)
			{
				attachedNeighborPowerSupplier = neighborPowerFinder.apply(mutaPos).apply(attachmentDirection);
				neighborPowerSuppliers.put(attachmentDirection, attachedNeighborPowerSupplier);
			}
			int weakNeighborPower = attachedNeighborState.getSignal(world, mutaPos, attachmentDirection);
			int neighborPower = this.useIndirectPower && attachedNeighborState.shouldCheckWeakPower(world, mutaPos, attachmentDirection)
				? Math.max(weakNeighborPower, world.getDirectSignalTo(mutaPos))
				: weakNeighborPower;
			power = Math.max(power, neighborPower*2 - 1);
			// always check channeled power of the face the subwire is attached to
			power = Math.max(power, attachedNeighborPowerSupplier.getPowerOnChannel(world, wirePos, wireState, attachmentDirection, channel)-1);
			// for the four sides that are neither attachmentSide nor its opposite:
			for (int orthagonal = 0; orthagonal < 4; orthagonal++)
			{
				int neighborSide = DirectionHelper.uncompressSecondSide(attachmentSide, orthagonal);
				Direction directionToNeighbor = Direction.from3DDataValue(neighborSide);

				Direction directionToWire = directionToNeighbor.getOpposite();
				BlockState neighborState = neighborStates[neighborSide];
				mutaPos.setWithOffset(wirePos, directionToNeighbor);
				if (neighborState == null)
				{
					neighborState = world.getBlockState(mutaPos);
					neighborStates[neighborSide] = neighborState;
				}
				Block neighborBlock = neighborState.getBlock();
				WireConnector wireConnector = wireConnectors.getOrDefault(neighborBlock, defaultWireConnector);
				if (wireConnector.canConnectToAdjacentWire(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToWire))
				{
					ExpandedPowerSupplier expandedPowerSupplier = expandedPowerSuppliers.getOrDefault(neighborBlock, defaultPowerSupplier);
					// power will always be at least 0 because it started at 0 and we're maxing against that
					int expandedWeakNeighborPower = expandedPowerSupplier.getExpandedPower(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToNeighbor);
					int expandedNeighborPower = this.useIndirectPower && neighborState.shouldCheckWeakPower(world, mutaPos, directionToNeighbor)
						? Math.max(expandedWeakNeighborPower, world.getDirectSignalTo(mutaPos)*2)
						: expandedWeakNeighborPower;
					power = Math.max(power, expandedNeighborPower-1);
				}
				// only check orthagonal capabilities if the cable can connect to that block
				WireConnector cableConnector = cableConnectors.getOrDefault(neighborBlock, defaultCableConnector);
				if (cableConnector.canConnectToAdjacentWire(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToWire))
				{
					ChanneledPowerSupplier orthagonalPowerSupplier;
					orthagonalPowerSupplier = neighborPowerSuppliers.get(directionToNeighbor);
					if (orthagonalPowerSupplier == null)
					{
						orthagonalPowerSupplier = neighborPowerFinder.apply(mutaPos).apply(directionToNeighbor);
						neighborPowerSuppliers.put(directionToNeighbor, orthagonalPowerSupplier);
					}
					// power will always be at least 0 because it started at 0 and we're maxing against that
					power = Math.max(power, orthagonalPowerSupplier.getPowerOnChannel(world, wirePos, wireState, attachmentDirection, channel)-1);
				}
				if (directionToNeighbor != attachmentDirection)
				{
					power = Math.max(power, wire.getPower(neighborSide) - 1);
				}
				// if we should check edge connections
				if (!attachedFaceStates[neighborSide] && neighborBlock == this && !neighborState.getValue(INTERIOR_FACES[attachmentSide]))
				{
					BlockPos diagonalPos = mutaPos.move(attachmentDirection);
					BlockState diagonalState = world.getBlockState(mutaPos);
					int directionToWireSide = directionToWire.ordinal();
					if (diagonalState.getBlock() == this && diagonalState.getValue(INTERIOR_FACES[directionToWireSide]))
					{
						BlockEntity diagonalTe = world.getBlockEntity(diagonalPos);
						if (diagonalTe instanceof WireBlockEntity)
						{	// at the moment we can only diagonally connect to blocks of the same type, just get the TE's power directly
							power = Math.max(power, ((WireBlockEntity)diagonalTe).getPower(directionToWireSide)-1);
						}
					}
				}
			}
			if (wire.setPower(attachmentSide, power))
			{
				anyPowerUpdated = true;
				// mark the face and the four orthagonal faces to it for updates
				Direction[] nextUpdateDirs = BlockStateUtil.OUTPUT_TABLE[attachmentSide];
				for (int i=0; i<4; i++)
				{
					Direction nextUpdateDir = nextUpdateDirs[i];
					if (attachedFaceStates[nextUpdateDir.ordinal()])
					{
						facesNeedingUpdates.add(nextUpdateDir);
					}
				}
			}
			
		}
		if (anyPowerUpdated && !world.isClientSide)
		{
			// we need to notify neighbors because changes to redstone output should count as block updates
			this.notifyNeighbors(world, wirePos, wireState, EnumSet.allOf(Direction.class), true);
		}
	}
}
