package commoble.morered.bitwise_logic;

import javax.annotation.Nonnull;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.plate_blocks.LogicFunction;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class SingleInputBitwiseLogicPlateBlock extends BitwiseLogicPlateBlock
{
	private final LogicFunction operator;

	public SingleInputBitwiseLogicPlateBlock(Properties properties, LogicFunction operator)
	{
		super(properties);
		this.operator = operator;
	}

	@Override
	protected void updatePower(World world, BlockPos thisPos, BlockState thisState)
	{
		TileEntity te = world.getBlockEntity(thisPos);
		if (te instanceof ChanneledPowerStorageTileEntity)
		{
			ChanneledPowerStorageTileEntity logicTE = (ChanneledPowerStorageTileEntity)te;
			// get capability from output side
			Direction outputDir = PlateBlockStateProperties.getOutputDirection(thisState);
			Direction inputDir = outputDir.getOpposite();
			BlockPos inputPos = thisPos.relative(inputDir);
			TileEntity inputTE = world.getBlockEntity(inputPos);
			byte[] power = new byte[16]; // defaults to 0s
			ChanneledPowerSupplier inputSupplier = inputTE == null
				? BitwiseLogicPlateBlock.NO_POWER_SUPPLIER
				: inputTE.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, inputDir.getOpposite()).orElse(NO_POWER_SUPPLIER);
			Direction attachmentDir = thisState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
			for (int i=0; i<16; i++)
			{
				byte inputPower = (byte)inputSupplier.getPowerOnChannel(world, thisPos, thisState, attachmentDir, i);
				boolean inputBit = inputPower > 0;
				boolean outputBit = this.operator.apply(false, inputBit, false);
				power[i] = (byte) (outputBit ? 31 : 0);
			}
			
			logicTE.setPower(power);
		}
	}

	
	public boolean canConnectToAdjacentCable(@Nonnull IBlockReader world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire)
	{
		Direction plateAttachmentDir = thisState.getValue(PlateBlock.ATTACHMENT_DIRECTION);
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(thisState);
		return plateAttachmentDir == wireFace &&
			(directionToWire == primaryOutputDirection || directionToWire == primaryOutputDirection.getOpposite());
	}

}
