package net.commoble.morered.bitwise_logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.TransmissionNode;
import net.commoble.morered.MoreRed;
import net.commoble.morered.plate_blocks.InputSide;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.util.BlockStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ThreeInputBitwiseGateBlockEntity extends BitwiseGateBlockEntity
{
	public static final String INPUT_A_KEY = "input_a";
	public static final String INPUT_B_KEY = "input_b";
	public static final String INPUT_C_KEY = "input_c";
	
	protected int[] inputs = new int[3];

	public ThreeInputBitwiseGateBlockEntity(BlockEntityType<? extends ThreeInputBitwiseGateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public static ThreeInputBitwiseGateBlockEntity create(BlockPos pos, BlockState state)
	{
		return new ThreeInputBitwiseGateBlockEntity(MoreRed.THREE_INPUT_BITWISE_GATE_BLOCK_ENTITY.get(), pos, state);
	}
	
	public void setInputOnChannel(InputSide side, boolean hasInput, int channel)
	{
		int newInput = this.inputs[side.ordinal()];
		if (hasInput)
		{
			newInput |= (1 << channel);
		}
		else
		{
			newInput &= ~(1 << channel);
		}
		this.setInput(side, newInput, false);
	}
	
	public void setInput(InputSide side, int newInput, boolean forceOutputUpdate)
	{
		int sideIndex = side.ordinal();
		int oldInput = this.inputs[sideIndex];
		this.inputs[sideIndex] = newInput;
		if (forceOutputUpdate || oldInput != newInput)
		{
			this.updateOutput();
			this.setChanged();
		}
	}

	@Override
	protected void resetOutput()
	{
		this.updateOutput();
	}
	
	public void updateOutput()
	{
		if (this.getBlockState().getBlock() instanceof ThreeInputBitwiseGateBlock block)
		{
			int output = block.operator.apply(this.inputs[0], this.inputs[1], this.inputs[2]);
			this.setOutput(output);
		}
	}

	@Override
	public void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);
		output.putInt(INPUT_A_KEY, this.inputs[0]);
		output.putInt(INPUT_B_KEY, this.inputs[1]);
		output.putInt(INPUT_C_KEY, this.inputs[2]);
	}

	@Override
	public void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		this.inputs = new int[] {
			input.getIntOr(INPUT_A_KEY, 0),
			input.getIntOr(INPUT_B_KEY, 0),
			input.getIntOr(INPUT_C_KEY, 0)
		};
	}

	@Override
	protected List<TransmissionNode> createInputNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel, int channelIndex, Direction attachmentDir, Direction outputDir)
	{
		int rotationIndex = state.getValue(PlateBlockStateProperties.ROTATION);
		Direction sideA = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.A.rotationsFromOutput);
		Direction sideB = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.B.rotationsFromOutput);
		Direction sideC = BlockStateUtil.getInputDirection(attachmentDir, rotationIndex, InputSide.C.rotationsFromOutput);
		NodeShape shapeA = NodeShape.ofSideSide(attachmentDir, sideA);
		NodeShape shapeB = NodeShape.ofSideSide(attachmentDir, sideB);
		NodeShape shapeC = NodeShape.ofSideSide(attachmentDir, sideC);
		NodeShape neighborShapeA = NodeShape.ofSideSide(attachmentDir, sideC);
		NodeShape neighborShapeB = NodeShape.ofSideSide(attachmentDir, sideB.getOpposite());
		NodeShape neighborShapeC = NodeShape.ofSideSide(attachmentDir, sideA);
		SignalGraphKey neighborKeyA = new SignalGraphKey(levelKey, pos.relative(sideA), neighborShapeA, channel);
		SignalGraphKey neighborKeyB = new SignalGraphKey(levelKey, pos.relative(sideB), neighborShapeB, channel);
		SignalGraphKey neighborKeyC = new SignalGraphKey(levelKey, pos.relative(sideC), neighborShapeC, channel);
		
		return List.of(
			new TransmissionNode(
				shapeA,
				BitwiseGateBlockEntity.INPUT_SOURCE,
				Set.of(sideA),
				Set.of(neighborKeyA),
				(levelAccess, power) -> {
					this.setInputOnChannel(InputSide.A, power > 0, channelIndex);
					return Map.of();
				}
			),
			new TransmissionNode(
				shapeB,
				BitwiseGateBlockEntity.INPUT_SOURCE,
				Set.of(sideB),
				Set.of(neighborKeyB),
				(levelAccess, power) -> {
					this.setInputOnChannel(InputSide.B, power > 0, channelIndex);
					return Map.of();
				}
			),
			new TransmissionNode(
				shapeC,
				BitwiseGateBlockEntity.INPUT_SOURCE,
				Set.of(sideC),
				Set.of(neighborKeyC),
				(levelAccess, power) -> {
					this.setInputOnChannel(InputSide.C, power > 0, channelIndex);
					return Map.of();
				}
			)
		);
	}

}
