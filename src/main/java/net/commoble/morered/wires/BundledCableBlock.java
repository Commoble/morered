package net.commoble.morered.wires;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.cache.LoadingCache;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.MoreRedAPI;
import net.commoble.morered.api.internal.WireVoxelHelpers;
import net.commoble.morered.future.Channel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BundledCableBlock extends AbstractWireBlock implements EntityBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = WireVoxelHelpers.makeNodeShapes(3, 4);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = WireVoxelHelpers.makeRaytraceBackboards(4);
	public static final VoxelShape[] LINE_SHAPES = WireVoxelHelpers.makeLineShapes(3, 4);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);

	public BundledCableBlock(Properties properties)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false, false, Channel.SIXTEEN_COLORS);
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().wireBeType.get().create(pos, state);
	}	
	
//	// invoked when an adjacent TE is marked dirty or an adjacent block updates its comparator output
//	@Override
//	public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor)
//	{
//		// when would we need to update power here?
//		// firstly, if the entire blockstate changed, we'll be updating the power elsewhere anyway
//		// so we only need to do extra things here if blockstate (and therefore the TE) remain the same,
//		// but the power input from the TE's capability changed
//		// we can assume that the *existence* of the neighbor TE and its capability itself won't change here
//		// so either A) the neighbor TE did not have the capability and will continue to not have it (or there is no TE there)
//		// in which case we don't need to do anything 
//		// or B) the neighbor TE does have the capability, in which case the power MAY have changed, in which case we should check power
//		
//		if (!(world instanceof Level level))
//			return;
//		
//		Direction directionFromNeighbor = DirectionHelper.getDirectionToNeighborPos(neighbor, pos);
//		if (directionFromNeighbor == null)
//			return;
//		
//		ChanneledPowerSupplier neighborPowerSupplier = level.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, neighbor, directionFromNeighbor);
//		if (neighborPowerSupplier != null)
//		{
//			this.updatePowerAfterBlockUpdate(level, pos, state);
//		}
//		
//		// if the changed neighbor has any convex edges through this block, propagate neighbor update along any edges
//		long edgeFlags = this.getEdgeFlags(world,pos);
//		if (edgeFlags != 0)
//		{
//			EnumSet<Direction> edgeUpdateDirs = EnumSet.noneOf(Direction.class);
//			Direction directionToNeighbor = directionFromNeighbor.getOpposite();
//			Edge[] edges = Edge.values();
//			for (int edgeFlag = 0; edgeFlag < 12; edgeFlag++)
//			{
//				if ((edgeFlags & (1 << edgeFlag)) != 0)
//				{
//					Edge edge = edges[edgeFlag];
//					if (edge.sideA == directionToNeighbor)
//						edgeUpdateDirs.add(edge.sideB);
//					else if (edge.sideB == directionToNeighbor)
//						edgeUpdateDirs.add(edge.sideA);
//				}
//			}
//			if (!edgeUpdateDirs.isEmpty())
//			{
//				BlockPos.MutableBlockPos mutaPos = pos.mutable();
//				for (Direction dir : edgeUpdateDirs)
//				{
//					BlockPos otherNeighborPos = mutaPos.setWithOffset(pos, dir);
//					world.getBlockState(otherNeighborPos).onNeighborChange(world, otherNeighborPos, pos);
//				}
//			}
//		}
//	}

//	@Override
//	protected void updatePowerAfterBlockUpdate(Level world, BlockPos wirePos, BlockState wireState)
//	{
////		BlockEntity te = world.getBlockEntity(wirePos);
////		if (!(te instanceof WireBlockEntity))
////			return; // if there's no TE then we can't make any updates
////		BundledCableBlockEntity wire = (BundledCableBlockEntity)te;
////
////		// keep in mind that we don't need to set neighbor updates here
////		// as calling markDirty notifies neighbors of TE updates
////		Map<Block,WireConnector> connectors = MoreRedAPI.getCableConnectabilityRegistry();
////		WireConnector defaultConnector = MoreRedAPI.getDefaultCableConnector();
////		
////		BlockPos.MutableBlockPos mutaPos = wirePos.mutable();
////		BlockState[] neighborStates = new BlockState[6];
////		Map<Direction,ChanneledPowerSupplier> neighborPowerSuppliers = new EnumMap<>(Direction.class);
////		ChanneledPowerSupplier noPower = DefaultWireProperties.NO_POWER_SUPPLIER;
////		Function<BlockPos, Function<Direction, ChanneledPowerSupplier>> neighborPowerFinder = neighborPos -> directionToNeighbor ->
////		{
////			ChanneledPowerSupplier neighborSupplier = world.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, neighborPos, directionToNeighbor.getOpposite());
////			return neighborSupplier == null ? noPower : neighborSupplier;
////		};
////		
////		for (int channel=0; channel<16; channel++)
////		{
////			
////			// set of faces needing updates
////			// ONLY faces of attached wires are to be added to this set (check the state before adding)
////			EnumSet<Direction> facesNeedingUpdates = EnumSet.noneOf(Direction.class);
////			boolean attachedFaceStates[] = new boolean[6];
////			for (int attachmentSide=0; attachmentSide<6; attachmentSide++)
////			{
////				if (wireState.getValue(INTERIOR_FACES[attachmentSide]))
////				{
////					attachedFaceStates[attachmentSide] = true;
////					facesNeedingUpdates.add(Direction.from3DDataValue(attachmentSide));
////				}
////				else
////				{
////					wire.setPower(attachmentSide, channel, 0);
////				}
////			}
////			
////			int iteration = 0;
////			while (!facesNeedingUpdates.isEmpty())
////			{
////				int attachmentSide = (iteration++) % 6;
////				Direction attachmentDirection = Direction.from3DDataValue(attachmentSide);
////				
////				// if the set of remaining faces doesn't contain the face, skip the rest of the iteration
////				if (!facesNeedingUpdates.remove(attachmentDirection)) 
////					continue;
////				
////				// we know there's a wire attached to the face because we checked the state before adding
////				// always check the capability of attached faces
////				mutaPos.setWithOffset(wirePos, attachmentDirection);
////				
////				// get neighbor state and power supplier in this direction, cacheing them for the rest of the method if we haven't yet
////				BlockState attachedNeighborState = neighborStates[attachmentSide];
////				if (attachedNeighborState == null)
////				{
////					attachedNeighborState = world.getBlockState(mutaPos);
////					neighborStates[attachmentSide] = attachedNeighborState;
////				}
////				ChanneledPowerSupplier attachedNeighborPowerSupplier;
////				attachedNeighborPowerSupplier = neighborPowerSuppliers.get(attachmentDirection);
////				if (attachedNeighborPowerSupplier == null)
////				{
////					attachedNeighborPowerSupplier = neighborPowerFinder.apply(mutaPos).apply(attachmentDirection);
////					neighborPowerSuppliers.put(attachmentDirection, attachedNeighborPowerSupplier);
////				}
////				int power = Math.max(0, attachedNeighborPowerSupplier.getPowerOnChannel(world, wirePos, wireState, attachmentDirection, channel)-1);
////				// for the four sides that are neither attachmentSide nor its opposite
////				for (int orthagonal = 0; orthagonal < 4; orthagonal++)
////				{
////					int neighborSide = DirectionHelper.uncompressSecondSide(attachmentSide, orthagonal);
////					Direction directionToNeighbor = Direction.from3DDataValue(neighborSide);
////
////					Direction directionToWire = directionToNeighbor.getOpposite();
////					BlockState neighborState = neighborStates[neighborSide];
////					mutaPos.setWithOffset(wirePos, directionToNeighbor);
////					if (neighborState == null)
////					{
////						neighborState = world.getBlockState(mutaPos);
////						neighborStates[neighborSide] = neighborState;
////					}
////					Block neighborBlock = neighborState.getBlock();
////					// only check orthagonal capabilities if the cable can connect to that block
////					WireConnector connector = connectors.getOrDefault(neighborBlock, defaultConnector);
////					if (connector.canConnectToAdjacentWire(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToWire))
////					{
////						ChanneledPowerSupplier orthagonalPowerSupplier;
////						orthagonalPowerSupplier = neighborPowerSuppliers.get(directionToNeighbor);
////						if (orthagonalPowerSupplier == null)
////						{
////							orthagonalPowerSupplier = neighborPowerFinder.apply(mutaPos).apply(directionToNeighbor);
////							neighborPowerSuppliers.put(directionToNeighbor, orthagonalPowerSupplier);
////						}
////						// power will always be at least 0 because it started at 0 and we're maxing against that
////						power = Math.max(power, orthagonalPowerSupplier.getPowerOnChannel(world, wirePos, wireState, attachmentDirection, channel)-1);
////					}
////					if (directionToNeighbor != attachmentDirection)
////					{
////						power = Math.max(power, wire.getPower(neighborSide, channel) - 1);
////					}
////					// if we should check edge connections
////					if (!attachedFaceStates[neighborSide] && neighborBlock == this && !neighborState.getValue(INTERIOR_FACES[attachmentSide]))
////					{
////						BlockPos diagonalPos = mutaPos.move(attachmentDirection);
////						BlockState diagonalState = world.getBlockState(mutaPos);
////						int directionToWireSide = directionToWire.ordinal();
////						if (diagonalState.getBlock() == this && diagonalState.getValue(INTERIOR_FACES[directionToWireSide]))
////						{
////							BlockEntity diagonalTe = world.getBlockEntity(diagonalPos);
////							if (diagonalTe instanceof BundledCableBlockEntity)
////							{
////								power = Math.max(power, ((BundledCableBlockEntity)diagonalTe).getPower(directionToWireSide, channel)-1);
////							}
////						}
////					}
////				}
////				if (wire.setPower(attachmentSide, channel, power))
////				{
////					// mark the face and the four orthagonal faces to it for updates
////					Direction[] nextUpdateDirs = BlockStateUtil.OUTPUT_TABLE[attachmentSide];
////					for (int i=0; i<4; i++)
////					{
////						Direction nextUpdateDir = nextUpdateDirs[i];
////						if (attachedFaceStates[nextUpdateDir.ordinal()])
////						{
////							facesNeedingUpdates.add(nextUpdateDir);
////						}
////					}
////				}
////				
////			}
////		}
//	}
	
	@Override
	protected void notifyNeighbors(Level world, BlockPos wirePos, BlockState newState, EnumSet<Direction> updateDirections, boolean doConductedPowerUpdates)
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
	protected boolean canAdjacentBlockConnectToFace(BlockGetter world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		return MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
			.canConnectToAdjacentWire(world, neighborPos, neighborState, thisPos, thisState, attachmentDirection, directionToWire);
	}

	@Override
	protected Set<Direction> onReceivePower(LevelAccessor level, BlockPos pos, BlockState state, Direction attachmentSide, int power, Channel channel, Set<Direction> relevantNeighbors)
	{
		return Set.of();
	}

}
