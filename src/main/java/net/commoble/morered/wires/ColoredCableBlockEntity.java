package net.commoble.morered.wires;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.ChanneledPowerSupplier;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredCableBlockEntity extends WireBlockEntity
{
	protected Map<Direction, ChanneledPowerSupplier> sidedPowerSuppliers = Util.make(new EnumMap<>(Direction.class), map ->
	{
		for (int i=0; i<6; i++)
		{
			Direction dir = Direction.from3DDataValue(i);
			map.put(dir, new SidedPowerSupplier(dir));
		}
	});
	
	public ColoredCableBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().coloredNetworkCableBeType.get(), pos, state);
	}
	
	public ColoredCableBlockEntity(BlockEntityType<ColoredCableBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public ChanneledPowerSupplier getChanneledPower(Direction side)
	{

		return side == null ? null : this.sidedPowerSuppliers.get(side);
	}
	
	protected class SidedPowerSupplier implements ChanneledPowerSupplier
	{
		private final Direction side;
		
		public SidedPowerSupplier(Direction side)
		{
			this.side = side;
		}

		@Override
		public int getPowerOnChannel(Level world, BlockPos wirePos, BlockState wireState, @Nullable Direction wireFace, int channel)
		{
			ColoredCableBlockEntity cable = ColoredCableBlockEntity.this;
			BlockState state = cable.getBlockState();
			Block block = state.getBlock();
			if (!(block instanceof ColoredCableBlock))
				return 0;
			
			if (channel != ((ColoredCableBlock)block).getDyeColor().ordinal())
				return 0;

			// check the power of the wire attached on the capability side first
			int sideIndex = this.side.ordinal();
			if (state.getValue(AbstractWireBlock.INTERIOR_FACES[sideIndex]))
				return cable.getPower(sideIndex);
			
			// otherwise, if the querier needs a specific wire face, get that power
			if (wireFace != null)
				return cable.getPower(wireFace.ordinal());
			
			// otherwise, get the strongest wire power among the four subwires adjacent to the capability side
			int output = 0;
			for (int subSide = 0; subSide < 4; subSide++)
			{
				int actualSubSide = DirectionHelper.uncompressSecondSide(sideIndex, subSide);
				if (state.getValue(AbstractWireBlock.INTERIOR_FACES[actualSubSide]))
					output = Math.max(output, cable.getPower(actualSubSide));
			}
			
			return output;
		}
		
	}

}
