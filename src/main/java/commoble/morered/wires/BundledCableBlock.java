package commoble.morered.wires;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

import com.google.common.cache.LoadingCache;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.util.DirectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class BundledCableBlock extends AbstractWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = AbstractWireBlock.makeNodeShapes(3, 4);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = AbstractWireBlock.makeRaytraceBackboards(4);
	public static final VoxelShape[] LINE_SHAPES = AbstractWireBlock.makeLineShapes(3, 4);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);

	public BundledCableBlock(Properties properties)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false);
	}


	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return TileEntityRegistrar.BUNDLED_NETWORK_CABLE.get().create();
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
		if (!(te instanceof BundledCableTileEntity))
			return; // if there's no TE then we can't make any updates
		BundledCableTileEntity wire = (BundledCableTileEntity)te;

		// keep in mind that we don't need to set neighbor updates here
		// as calling markDirty notifies neighbors of TE updates
		Map<Block,WireConnector> connectors = MoreRedAPI.getCableConnectabilityRegistry();
		WireConnector defaultConnector = MoreRedAPI.getDefaultCableConnector();
		
		BlockPos.Mutable mutaPos = wirePos.toMutable();
		BlockState[] neighborStates = new BlockState[6];
		Map<Direction,ChanneledPowerSupplier> neighborPowerSuppliers = new EnumMap<>(Direction.class);
		ChanneledPowerSupplier noPower = DefaultWireProperties.NO_POWER_SUPPLIER;
		Function<BlockPos, Function<Direction, ChanneledPowerSupplier>> neighborPowerFinder = neighborPos -> directionToNeighbor ->
		{
			TileEntity neighborTE = world.getTileEntity(neighborPos);
			if (neighborTE == null)
				return noPower;
			
			return neighborTE.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, directionToNeighbor.getOpposite()).orElse(noPower);
		};
		
		for (int channel=0; channel<16; channel++)
		{
			
			// set of faces needing updates
			// ONLY faces of attached wires are to be added to this set (check the state before adding)
			EnumSet<Direction> facesNeedingUpdates = EnumSet.noneOf(Direction.class);
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
					wire.setPower(attachmentSide, channel, 0);
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
				// always check the capability of attached faces
				mutaPos.setAndMove(wirePos, attachmentDirection);
				
				// get neighbor state and power supplier in this direction, cacheing them for the rest of the method if we haven't yet
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
				int power = Math.max(0, attachedNeighborPowerSupplier.getPowerOnChannel(world, wirePos, wireState, attachmentDirection, channel)-1);
				// for the four sides that are neither attachmentSide nor its opposite
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
					// only check orthagonal capabilities if the cable can connect to that block
					WireConnector connector = connectors.getOrDefault(neighborBlock, defaultConnector);
					if (connector.canConnectToAdjacentWire(world, wirePos, wireState, attachmentDirection, directionToWire, mutaPos, neighborState))
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
						power = Math.max(power, wire.getPower(neighborSide, channel) - 1);
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
							if (diagonalTe instanceof BundledCableTileEntity)
							{
								power = Math.max(power, ((BundledCableTileEntity)diagonalTe).getPower(directionToWireSide, channel)-1);
							}
						}
					}
				}
				if (wire.setPower(attachmentSide, channel, power))
				{
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
		}
	}
	
	@Override
	protected void notifyNeighbors(World world, BlockPos wirePos, BlockState newState, EnumSet<Direction> updateDirections, boolean doConductedPowerUpdates)
	{
//		BlockPos.Mutable mutaPos = wirePos.toMutable();
//		Block newBlock = newState.getBlock();
//		if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, wirePos, newState, updateDirections, false).isCanceled())
//		{
//			// if either the old block or new block emits strong power,
//			// and a given neighbor block conducts strong power,
//			// then we should notify second-degree neighbors as well 
//			for (Direction dir : updateDirections)
//			{
//				BlockPos neighborPos = mutaPos.setAndMove(wirePos, dir);
//				boolean doSecondaryNeighborUpdates = doConductedPowerUpdates && world.getBlockState(neighborPos).shouldCheckWeakPower(world, neighborPos, dir);
//				world.neighborChanged(neighborPos, newBlock, wirePos);
//				if (doSecondaryNeighborUpdates)
//					world.notifyNeighborsOfStateChange(neighborPos, newBlock);
//			}
//		}
	}

	@Override
	protected boolean canAdjacentBlockConnectToFace(IBlockReader world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		return MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
			.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState);
	}

}
