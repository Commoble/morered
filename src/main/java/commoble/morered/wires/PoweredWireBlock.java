package commoble.morered.wires;

import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.cache.LoadingCache;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ExpandedPowerSupplier;
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
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

public abstract class PoweredWireBlock extends AbstractWireBlock
{
	
	public PoweredWireBlock(Properties properties, VoxelShape[] shapesByStateIndex, VoxelShape[] raytraceBackboards, LoadingCache<Long, VoxelShape> voxelCache, boolean useIndirectPower)
	{
		super(properties, shapesByStateIndex, raytraceBackboards, voxelCache, useIndirectPower);
	}


	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return TileEntityRegistrar.WIRE.get().create();
	}
	
	@Override
	public boolean isSignalSource(BlockState state)
	{
		return !this.isEmptyWireBlock(state);
	}
	
	// the difference between strong power and weak power is mostly to do with the way power is conducted through solid blocks
	// when the world is queried for power output from a position, it checks if that position is a solid cube
		// forge patches the world to check shouldCheckWeakPower so blocks can "conduct" indirect power even if they're not a solid cube
	// if the block isn't a solid cube / doesn't conduct, we check weak power output of the block
	// if the block is a solid cube / does conduct, we check strong power output of blocks adjacent to the cube
	// so generally, if we supply strong power, we should also supply weak power for consistency
	// but if we supply weak power, we don't necessarily have to supply strong power

	// get "immediate power"
	@Override
	public int getSignal(BlockState state, IBlockReader world, BlockPos pos, Direction directionFromNeighbor)
	{
		return this.getPower(state,world,pos,directionFromNeighbor,false);
	}

	// get the power that can be conducted through solid blocks
	@Override
	public int getDirectSignal(BlockState state, IBlockReader world, BlockPos pos, Direction directionFromNeighbor)
	{
		return this.useIndirectPower ? this.getPower(state,world,pos,directionFromNeighbor,false) : 0;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction directionFromNeighbor)
	{
		// connectability is fundamentally sided
		// if no side is supplied, we can't connect
		if (directionFromNeighbor == null)
			return false;
		
		Direction directionToNeighbor = directionFromNeighbor.getOpposite();
		int side = directionToNeighbor.ordinal();
		
		// if we have a wire attached to the given side, we can connect
		if (state.getValue(INTERIOR_FACES[side]))
			return true;
		
		// otherwise, check the four orthagonal faces relative to the neighbor block
		// this.canConnectRedstone is used when the other state wants to know whether it can potentially receive redstone from this block
		for (int subSide = 0; subSide < 4; subSide++)
		{
			int orthagonalSide = DirectionHelper.uncompressSecondSide(side, subSide);
			// if the projected neighbor shape entirely overlaps the line shape,
			// then the neighbor shape can be connected to by the wire
			// we can test this by doing an ONLY_SECOND comparison on the shapes
			// if this returns true, then there are places where the second shape is not overlapped by the first
			// so if this returns false, then we can proceed
			if (state.getValue(INTERIOR_FACES[orthagonalSide]))
			{
				return true;
			}
		}
		
		return false;
	}
	
	protected int getPower(BlockState state, IBlockReader world, BlockPos pos, Direction directionFromNeighbor, boolean expand)
	{
//		return 0;
		// power is stored in the TE (because storing it in 16 states per side is too many state combinations)
		// if we don't have a TE, we have no power
		TileEntity te = world.getBlockEntity(pos);
		if (!(te instanceof WireTileEntity))
			return 0;
		WireTileEntity wire = (WireTileEntity)te;
		
		Direction directionToNeighbor = directionFromNeighbor.getOpposite();
		int side = directionToNeighbor.ordinal();
		
		// if we have a wire attached on the given side, always use the power value of that wire
		if (state.getValue(INTERIOR_FACES[side]))
		{
			int power = wire.getPower(side);
			return expand ? power : power/2;
		}
		
		// otherwise, check the four orthagonal attachment faces and use the highest power of the side-connectable wires
		BlockPos neighborPos = pos.relative(directionToNeighbor);
		BlockState neighborState = world.getBlockState(neighborPos);
		WireConnector connector = MoreRedAPI.getWireConnectabilityRegistry().getOrDefault(neighborState.getBlock(), MoreRedAPI.getDefaultWireConnector());
		int output = 0;
		for (int i=0; i<4; i++)
		{
			int attachmentSide = DirectionHelper.uncompressSecondSide(side, i);
			Direction attachmentDirection = Direction.from3DDataValue(attachmentSide);
			if (state.getValue(INTERIOR_FACES[attachmentSide]) && connector.canConnectToAdjacentWire(world, neighborPos, neighborState, pos, state, attachmentDirection, directionFromNeighbor))
			{
				output = Math.max(output, wire.getPower(attachmentSide));
			}
		}
		return expand ? output : output/2;
	}
	
	public int getExpandedPower(World world, BlockPos thisPos, BlockState thisState, BlockPos wirePos, BlockState wireState,
		Direction wireFace, Direction directionFromWire)
	{
		TileEntity te = world.getBlockEntity(thisPos);
		if (!(te instanceof WireTileEntity))
			return 0; // if there's no TE then we can't make power
		WireTileEntity wire = (WireTileEntity)te;
		return thisState.getValue(INTERIOR_FACES[wireFace.ordinal()])
			? wire.getPower(wireFace)
			: 0;
	}


	@Override
	protected void updatePowerAfterBlockUpdate(World world, BlockPos wirePos, BlockState wireState)
	{
		TileEntity te = world.getBlockEntity(wirePos);
		if (!(te instanceof WireTileEntity))
			return; // if there's no TE then we can't make any updates
		WireTileEntity wire = (WireTileEntity)te;
		
//		EnumSet<Direction> updatedDirections = EnumSet.noneOf(Direction.class);
		Map<Block,WireConnector> connectors = MoreRedAPI.getWireConnectabilityRegistry();
		Map<Block,ExpandedPowerSupplier> expandedPowerSuppliers = MoreRedAPI.getExpandedPowerRegistry();
		WireConnector defaultConnector = MoreRedAPI.getDefaultWireConnector();
		ExpandedPowerSupplier defaultPowerSupplier = MoreRedAPI.getDefaultExpandedPowerSupplier();
		
		BlockPos.Mutable mutaPos = wirePos.mutable();
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
			BlockState attachedNeighborState;
			BlockState attachedNeighborStateCheck = neighborStates[attachmentSide];
			if (attachedNeighborStateCheck == null)
			{
				attachedNeighborState = world.getBlockState(mutaPos);
				neighborStates[attachmentSide] = attachedNeighborState;
			}
			else
			{
				attachedNeighborState = attachedNeighborStateCheck;
			}
			int weakNeighborPower = attachedNeighborState.getSignal(world, mutaPos, attachmentDirection);
			int neighborPower = this.useIndirectPower && attachedNeighborState.shouldCheckWeakPower(world, mutaPos, attachmentDirection)
				? Math.max(weakNeighborPower, world.getDirectSignalTo(mutaPos))
				: weakNeighborPower;
			power = Math.max(power, neighborPower*2 - 1);
			// for the four sides that are neither attachmentSide nor its opposite
			for (int orthagonal = 0; orthagonal < 4; orthagonal++)
			{
				int neighborSide = DirectionHelper.uncompressSecondSide(attachmentSide, orthagonal);
				Direction directionToNeighbor = Direction.from3DDataValue(neighborSide);

				Direction directionToWire = directionToNeighbor.getOpposite();
				BlockState neighborState;
				BlockState neighborStateCheck = neighborStates[neighborSide];
				mutaPos.setWithOffset(wirePos, directionToNeighbor);
				if (neighborStateCheck == null)
				{
					neighborState = world.getBlockState(mutaPos);
					neighborStates[neighborSide] = neighborState;
				}
				else
				{
					neighborState = neighborStateCheck;
				}
				Block neighborBlock = neighborState.getBlock();
				WireConnector connector = connectors.getOrDefault(neighborBlock, defaultConnector);
				if (connector.canConnectToAdjacentWire(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToWire))
				{
					ExpandedPowerSupplier expandedPowerSupplier = expandedPowerSuppliers.getOrDefault(neighborBlock, defaultPowerSupplier);
					// power will always be at least 0 because it started at 0 and we're maxing against that
					int expandedWeakNeighborPower = expandedPowerSupplier.getExpandedPower(world, mutaPos, neighborState, wirePos, wireState, attachmentDirection, directionToNeighbor);
					int expandedNeighborPower = this.useIndirectPower && neighborState.shouldCheckWeakPower(world, mutaPos, directionToNeighbor)
						? Math.max(expandedWeakNeighborPower, world.getDirectSignalTo(mutaPos)*2)
						: expandedWeakNeighborPower;
					power = Math.max(power, expandedNeighborPower-1);
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
						TileEntity diagonalTe = world.getBlockEntity(diagonalPos);
						if (diagonalTe instanceof WireTileEntity)
						{
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
//					updatedDirections.add(nextUpdateDir);
				}
//				updatedDirections.add(attachmentDirection);
			}
			
		}
		if (anyPowerUpdated && !world.isClientSide)
		{
			this.notifyNeighbors(world, wirePos, wireState, EnumSet.allOf(Direction.class), true);
		}
	}
	
	@Override
	protected void notifyNeighbors(World world, BlockPos wirePos, BlockState newState, EnumSet<Direction> updateDirections, boolean doConductedPowerUpdates)
	{
		BlockPos.Mutable mutaPos = wirePos.mutable();
		Block newBlock = newState.getBlock();
		if (!net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, wirePos, newState, updateDirections, false).isCanceled())
		{
			// if either the old block or new block emits strong power,
			// and a given neighbor block conducts strong power,
			// then we should notify second-degree neighbors as well
			for (Direction dir : updateDirections)
			{
				BlockPos neighborPos = mutaPos.setWithOffset(wirePos, dir);
				boolean doSecondaryNeighborUpdates = doConductedPowerUpdates && world.getBlockState(neighborPos).shouldCheckWeakPower(world, neighborPos, dir);
				world.neighborChanged(neighborPos, newBlock, wirePos);
				if (doSecondaryNeighborUpdates)
					world.updateNeighborsAt(neighborPos, newBlock);
			}
		}
	}
}
