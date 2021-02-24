package commoble.morered.wires;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

import com.google.common.cache.LoadingCache;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.ExpandedPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.util.DirectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.DyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class ColoredCableBlock extends PoweredWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = AbstractWireBlock.makeNodeShapes(2, 3);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = AbstractWireBlock.makeRaytraceBackboards(3);
	public static final VoxelShape[] LINE_SHAPES = AbstractWireBlock.makeLineShapes(2, 3);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);
	
	private final DyeColor color; public DyeColor getDyeColor() { return this.color; }

	public ColoredCableBlock(Properties properties, DyeColor color)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false);
		this.color = color;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return TileEntityRegistrar.COLORED_NETWORK_CABLE.get().create();
	}

	public boolean canConnectToAdjacentWireOrCable(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos neighborPos,
		BlockState neighborState)
	{
		Block wireBlock = wireState.getBlock();
		return wireBlock instanceof ColoredCableBlock && ((ColoredCableBlock)wireBlock).getDyeColor() != this.getDyeColor()
			? false
			: AbstractWireBlock.canWireConnectToAdjacentWireOrCable(world, wirePos, wireState, wireFace, directionToWire, neighborPos, neighborState);
	}

	@Override
	protected boolean canAdjacentBlockConnectToFace(IBlockReader world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		// do this check first so we don't check the same behaviour twice for colored cable blocks
		return neighborBlock instanceof ColoredCableBlock
			? ((ColoredCableBlock)neighborBlock).canConnectToAdjacentWireOrCable(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState)
			: MoreRedAPI.getWireConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultWireConnector())
				.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState)
				||
				MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
				.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState);
	}
	
	// invoked when an adjacent TE is marked dirty or an adjacent block updates its comparator output
	@Override
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor)
	{
		// when would we need to update power here?
		// firstly, if the entire blockstate changed, we'll be updating the power elsewhere anyway
		// so we only need to do extra things here if blockstate (and therefore the TE) remain the same,
		// but the power input from the TE's capability changed
		// we can assume that the *existence* of the neighbor TE and its capability itself won't change here
		// so either A) the neighbor TE did not have the capability and will continue to not have it (or there is no TE there)
		// in which case we don't need to do anything 
		// or B) the neighbor TE does have the capability, in which case the power MAY have changed, in which case we should check power
		
		if (!(world instanceof World))
			return;
		
		TileEntity neighborTE = world.getTileEntity(pos);
		if (neighborTE == null)
			return;
		
		Direction directionFromNeighbor = DirectionHelper.getDirectionToNeighborPos(neighbor, pos);
		if (directionFromNeighbor == null)
			return;
		
		neighborTE.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, directionFromNeighbor).ifPresent($ -> this.updatePowerAfterBlockUpdate((World)world, pos, state));
	}
	
	@Override
	protected void updatePowerAfterBlockUpdate(World world, BlockPos wirePos, BlockState wireState)
	{
		TileEntity te = world.getTileEntity(wirePos);
		if (!(te instanceof WireTileEntity))
			return; // if there's no TE then we can't make any updates
		WireTileEntity wire = (WireTileEntity)te;
		
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
			TileEntity neighborTE = world.getTileEntity(neighborPos);
			if (neighborTE == null)
				return noPower;
			
			return neighborTE.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, directionToNeighbor.getOpposite()).orElse(noPower);
		};
		int channel = this.getDyeColor().ordinal();
		
		BlockPos.Mutable mutaPos = wirePos.toMutable();
		BlockState[] neighborStates = new BlockState[6];
		
		boolean anyPowerUpdated = false;
		
		// set of faces needing updates
		// ONLY faces of attached wires are to be added to this set (check the state before adding)
		EnumSet<Direction> facesNeedingUpdates = EnumSet.noneOf(Direction.class);
//		Set<BlockPos> diagonalNeighborsToUpdate = new HashSet<>();
		boolean attachedFaceStates[] = new boolean[6];
		for (int attachmentSide=0; attachmentSide<6; attachmentSide++)
		{
			if (wireState.get(INTERIOR_FACES[attachmentSide]))
			{
				attachedFaceStates[attachmentSide] = true;
				facesNeedingUpdates.add(Direction.byIndex(attachmentSide));
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
			Direction attachmentDirection = Direction.byIndex(attachmentSide);
			
			// if the set of remaining faces doesn't contain the face, skip the rest of the iteration
			if (!facesNeedingUpdates.remove(attachmentDirection)) 
				continue;
			
			// we know there's a wire attached to the face because we checked the state before adding
			int power = 0;
			// always get power from attached faces, those are full cubes and may supply conducted power
			mutaPos.setAndMove(wirePos, attachmentDirection);
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
			int weakNeighborPower = attachedNeighborState.getWeakPower(world, mutaPos, attachmentDirection);
			int neighborPower = this.useIndirectPower && attachedNeighborState.shouldCheckWeakPower(world, mutaPos, attachmentDirection)
				? Math.max(weakNeighborPower, world.getStrongPower(mutaPos))
				: weakNeighborPower;
			power = Math.max(power, neighborPower*2 - 1);
			// always check channeled power of the face the subwire is attached to
			power = Math.max(power, attachedNeighborPowerSupplier.getPowerOnChannel(world, wirePos, wireState, attachmentDirection, channel)-1);
			// for the four sides that are neither attachmentSide nor its opposite:
			for (int orthagonal = 0; orthagonal < 4; orthagonal++)
			{
				int neighborSide = DirectionHelper.uncompressSecondSide(attachmentSide, orthagonal);
				Direction directionToNeighbor = Direction.byIndex(neighborSide);

				Direction directionToWire = directionToNeighbor.getOpposite();
				BlockState neighborState = neighborStates[neighborSide];
				mutaPos.setAndMove(wirePos, directionToNeighbor);
				if (neighborState == null)
				{
					neighborState = world.getBlockState(mutaPos);
					neighborStates[neighborSide] = neighborState;
				}
				Block neighborBlock = neighborState.getBlock();
				WireConnector wireConnector = wireConnectors.getOrDefault(neighborBlock, defaultWireConnector);
				if (wireConnector.canConnectToAdjacentWire(world, wirePos, wireState, attachmentDirection, directionToWire, mutaPos, neighborState))
				{
					ExpandedPowerSupplier expandedPowerSupplier = expandedPowerSuppliers.getOrDefault(neighborBlock, defaultPowerSupplier);
					// power will always be at least 0 because it started at 0 and we're maxing against that
					int expandedWeakNeighborPower = expandedPowerSupplier.getExpandedPower(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToNeighbor);
					int expandedNeighborPower = this.useIndirectPower && neighborState.shouldCheckWeakPower(world, mutaPos, directionToNeighbor)
						? Math.max(expandedWeakNeighborPower, world.getStrongPower(mutaPos)*2)
						: expandedWeakNeighborPower;
					power = Math.max(power, expandedNeighborPower-1);
				}
				// only check orthagonal capabilities if the cable can connect to that block
				WireConnector cableConnector = cableConnectors.getOrDefault(neighborBlock, defaultCableConnector);
				if (cableConnector.canConnectToAdjacentWire(world, wirePos, wireState, attachmentDirection, directionToWire, mutaPos, neighborState))
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
				if (!attachedFaceStates[neighborSide] && neighborBlock == this && !neighborState.get(INTERIOR_FACES[attachmentSide]))
				{
					BlockPos diagonalPos = mutaPos.move(attachmentDirection);
					BlockState diagonalState = world.getBlockState(mutaPos);
					int directionToWireSide = directionToWire.ordinal();
					if (diagonalState.getBlock() == this && diagonalState.get(INTERIOR_FACES[directionToWireSide]))
					{
						TileEntity diagonalTe = world.getTileEntity(diagonalPos);
						if (diagonalTe instanceof WireTileEntity)
						{	// at the moment we can only diagonally connect to blocks of the same type, just get the TE's power directly
							power = Math.max(power, ((WireTileEntity)diagonalTe).getPower(directionToWireSide)-1);
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
		if (anyPowerUpdated && !world.isRemote)
		{
			// we need to notify neighbors because changes to redstone output should count as block updates
			this.notifyNeighbors(world, wirePos, wireState, EnumSet.allOf(Direction.class), true);
		}
	}
}
