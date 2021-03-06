package commoble.morered.bitwise_logic;

import javax.annotation.Nonnull;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.plate_blocks.InputSide;
import commoble.morered.plate_blocks.LogicFunction;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.util.BlockStateUtil;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class TwoInputBitwiseLogicPlateBlock extends BitwiseLogicPlateBlock
{
	private final LogicFunction operator;

	public TwoInputBitwiseLogicPlateBlock(Properties properties, LogicFunction operator)
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
			byte[] power = new byte[16]; // defaults to 0s
			Direction attachmentDir = thisState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
			int rotationIndex = thisState.getValue(PlateBlockStateProperties.ROTATION);
			Direction inputSideA = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
			Direction inputSideC = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
			TileEntity inputTileA = world.getBlockEntity(thisPos.relative(inputSideA));
			TileEntity inputTileC = world.getBlockEntity(thisPos.relative(inputSideC));
			ChanneledPowerSupplier inputA = inputTileA == null
				? BitwiseLogicPlateBlock.NO_POWER_SUPPLIER
				: inputTileA.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, inputSideA.getOpposite()).orElse(NO_POWER_SUPPLIER);
			ChanneledPowerSupplier inputC = inputTileC == null
				? BitwiseLogicPlateBlock.NO_POWER_SUPPLIER
				: inputTileC.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, inputSideC.getOpposite()).orElse(NO_POWER_SUPPLIER);
			for (int i=0; i<16; i++)
			{
				boolean inputBitA = inputA.getPowerOnChannel(world, thisPos, thisState, attachmentDir, i) > 0;
				boolean inputBitC = inputC.getPowerOnChannel(world, thisPos, thisState, attachmentDir, i) > 0;
				boolean outputBit = this.operator.apply(inputBitA, false, inputBitC);
				power[i] = (byte) (outputBit ? 31 : 0);
			}
			
			logicTE.setPower(power);
		}
	}

	
	@Override
	public boolean canConnectToAdjacentCable(@Nonnull IBlockReader world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire)
	{
		Direction plateAttachmentDir = thisState.getValue(PlateBlock.ATTACHMENT_DIRECTION);
		Direction primaryOutputDirection = PlateBlockStateProperties.getOutputDirection(thisState);
		Direction attachmentDir = thisState.getValue(PlateBlockStateProperties.ATTACHMENT_DIRECTION);
		int rotationIndex = thisState.getValue(PlateBlockStateProperties.ROTATION);
		Direction inputSideA = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
		Direction inputSideC = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
		return plateAttachmentDir == wireFace &&
			(directionToWire == primaryOutputDirection || directionToWire == inputSideA || directionToWire == inputSideC);
	}

}
