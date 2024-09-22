package net.commoble.morered.wires;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.cache.LoadingCache;

import net.commoble.morered.MoreRed;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.SignalStrength;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class PoweredWireBlock extends AbstractWireBlock implements EntityBlock
{
	
	public PoweredWireBlock(Properties properties, VoxelShape[] shapesByStateIndex, VoxelShape[] raytraceBackboards, LoadingCache<Long, VoxelShape> voxelCache, boolean useIndirectPower, boolean readAttachedPower, Collection<Channel> channels)
	{
		super(properties, shapesByStateIndex, raytraceBackboards, voxelCache, readAttachedPower, true, useIndirectPower, channels);
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().poweredWireBeType.get().create(pos, state);
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
	public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction directionFromNeighbor)
	{
		return this.getPower(state,world,pos,directionFromNeighbor);
	}

	// get the power that can be conducted through solid blocks
	@Override
	public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction directionFromNeighbor)
	{
		return this.useIndirectPower ? this.getPower(state,world,pos,directionFromNeighbor) : 0;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction directionFromNeighbor)
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
	
	protected int getPower(BlockState state, BlockGetter world, BlockPos pos, Direction directionFromNeighbor)
	{
//		return 0;
		// power is stored in the TE (because storing it in 16 states per side is too many state combinations)
		// if we don't have a TE, we have no power
		BlockEntity te = world.getBlockEntity(pos);
		if (!(te instanceof PoweredWireBlockEntity wire))
			return 0;
		
		Direction directionToNeighbor = directionFromNeighbor.getOpposite();
		int side = directionToNeighbor.ordinal();
		
		// if we have a wire attached on the given side, always use the power value of that wire
		if (state.getValue(INTERIOR_FACES[side]))
		{
			return wire.getPower(side);
		}
		
		// otherwise, check the four orthagonal attachment faces and use the highest power of the side-connectable wires
		long connections = wire.getConnections();
		int output = 0;
		for (Direction attachmentSide : Direction.values())
		{
			if (attachmentSide == directionToNeighbor || attachmentSide == directionFromNeighbor)
				continue;
			int attachmentIndex = attachmentSide.ordinal();
			int secondarySide = DirectionHelper.getCompressedSecondSide(attachmentIndex, side);
			if (DirectionHelper.hasLineConnection(connections, attachmentIndex, secondarySide))
			{
				output = Math.max(output, wire.getPower(attachmentIndex));
			}
		}
		return output;
	}

	@Override
	protected Map<Direction, SignalStrength> onReceivePower(LevelAccessor level, BlockPos pos, BlockState state, Direction attachmentSide, int power, Channel channel)
	{
		if (!(level.getBlockEntity(pos) instanceof PoweredWireBlockEntity wire) || wire.getPower(attachmentSide) == power)
			return Map.of();
		
		wire.setPower(attachmentSide, power);
		Map<Direction,SignalStrength> map = new HashMap<>();
		SignalStrength strength = this.useIndirectPower ? SignalStrength.STRONG : SignalStrength.WEAK;
		for (Direction dir : Direction.values())
		{
			map.put(dir, strength);
		}
		return map;
	}

	@Override
	public void onRemove(BlockState oldState, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		super.onRemove(oldState, worldIn, pos, newState, isMoving);
		if (this.notifyAttachedNeighbors && this.useIndirectPower)
		{
			// after doing graph updates, we may need to proc additional diagonal updates on remove
			for (Direction directionToNeighbor : Direction.values())
			{
				BlockPos neighborPos = pos.relative(directionToNeighbor);
				worldIn.updateNeighborsAtExceptFromFacing(neighborPos, newState.getBlock(), directionToNeighbor.getOpposite());
			}
		}
	}
	
	
}
