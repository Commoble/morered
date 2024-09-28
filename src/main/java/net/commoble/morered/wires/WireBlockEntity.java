package net.commoble.morered.wires;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.commoble.morered.client.WirePartModelLoader.WireModelData;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.ChannelSet;
import net.commoble.morered.future.TransmissionNode;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class WireBlockEntity extends BlockEntity
{
	public static final String CONNECTIONS = "connections";
	protected long connections = 0;
	protected Map<Direction, Map<Channel, TransmissionNode>> nodes = null;
	
	public WireBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().wireBeType.get(), pos, state);
	}
	
	public WireBlockEntity(BlockEntityType<? extends WireBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public long getConnections()
	{
		return this.connections;
	}
	
	public void setConnectionsWithServerUpdate(long connections)
	{
		this.setConnectionsAndUpdateNodes(connections);
		this.setChanged();
		this.level.sendBlockUpdated(this.getBlockPos(), getBlockState(), getBlockState(), 0);
	}
	
	public void setConnectionsAndUpdateNodes(long connections)
	{
		this.connections = connections;
		this.nodes = null;
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		// normalize connection flags
		long newConnections = 0L;
		if (this.connections != 0)
		{
			OctahedralGroup normalizer = this.getBlockState().getValue(AbstractWireBlock.TRANSFORM).inverse();
			newConnections = transformConnectionFlags(this.connections, normalizer);
		}
		compound.putLong(CONNECTIONS, newConnections);
	}
	
	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries);
		long normalizedConnections = compound.getLong(CONNECTIONS);
		long newConnections = 0L;
		if (normalizedConnections != 0L)
		{
			OctahedralGroup denormalizer = this.getBlockState().getValue(AbstractWireBlock.TRANSFORM);
			newConnections = transformConnectionFlags(normalizedConnections, denormalizer);
		}
		this.connections = newConnections;
	}

	// called on server to get the data to send to clients when chunk is loaded for client
	// defaults to this.writeInternal()
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag compound = super.getUpdateTag(registries); // empty tag
		this.saveAdditional(compound, registries);
		return compound;
	}

	// called on server when world.notifyBlockUpdate is invoked at this TE's position
	// generates the packet to send to nearby clients
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this); // calls getUpdateTag
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, Provider lookupProvider)
	{
		super.handleUpdateTag(tag, lookupProvider);
		this.requestModelDataUpdate();
		BlockState state = this.getBlockState();
		this.level.sendBlockUpdated(this.worldPosition, state, state, 8);
	}

	// called on client to read the packet sent by getUpdatePacket
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries)
	{
		long oldConnections = this.connections;
		super.onDataPacket(net, pkt, registries);	// calls load()
		BlockState state = this.getBlockState();
		if (oldConnections != this.connections)
		{
			VoxelCache.invalidate(level, worldPosition);
			this.requestModelDataUpdate();
		}
		this.level.sendBlockUpdated(this.worldPosition, state, state, 8);
	}
	
	public Map<Channel, TransmissionNode> getTransmissionNodes(Direction attachmentSide, BlockState newState)
	{
		if (this.nodes == null)
		{
			this.nodes = this.createNodes(newState);
		}
		@Nullable Map<Channel, TransmissionNode> nodes = this.nodes.get(attachmentSide);
		return nodes == null ? Map.of() : nodes;
	}

	@Override
	public ModelData getModelData()
	{
		return ModelData.builder()
			.with(WireModelData.PROPERTY, this.connections)
			.build();
	}
	
	protected Map<Direction, Map<Channel, TransmissionNode>> createNodes(BlockState newState)
	{
		if (!(newState.getBlock() instanceof AbstractWireBlock wireBlock))
			return Map.of();
		
		return wireBlock.createNodes(this.level, this.worldPosition, newState);
	}
	
	protected ChannelSet getChannels()
	{
		return this.getBlockState().getBlock() instanceof AbstractWireBlock block
			? block.getChannels()
			: ChannelSet.EMPTY;
	}
	
	public static long transformConnectionFlags(long oldConnections, OctahedralGroup transform)
	{
		long newConnections = 0L;
		// first six flags are just directions
		for (int attachmentSideIndex=0; attachmentSideIndex<6; attachmentSideIndex++)
		{
			if ((oldConnections & (1L << attachmentSideIndex)) != 0L)
			{
				newConnections |= (1L << (transform.rotate(Direction.from3DDataValue(attachmentSideIndex)).ordinal()));
			}
		}
		// next 24 flags are line flags (6*4)
		// 4 flags per attachment side, DDDDUUUUNNNNSSSSWWWWEEEE (D is LSB)
		// how do we rotate a line 90 degrees around Y?
		// the attachment index rotates as above, the rotation index is trickier
		// well, no
		// given the index, we can calculate the secondary direction
		// and then just rotate that
		// e.g. for 90 degrees
		// DOWN+NORTH becomes DOWN+EAST
		// UP+NORTH BECOMES UP+EAST
		// EAST+UP becomes SOUTH+UP
		// attachment side has no effect on the secondary side's normalization
		for (int lineAttachmentIndex=0; lineAttachmentIndex<6; lineAttachmentIndex++)
		{
			Direction primaryDirection = Direction.from3DDataValue(lineAttachmentIndex);
			int newPrimary = transform.rotate(primaryDirection).ordinal();
			for (int lineRotationIndex=0; lineRotationIndex<4; lineRotationIndex++)
			{
				if ((oldConnections & (1L << (((lineAttachmentIndex*4) + lineRotationIndex) + 6))) != 0)
				{
					int secondaryDirectionIndex = DirectionHelper.uncompressSecondSide(lineAttachmentIndex, lineRotationIndex);
					Direction secondaryDirection = Direction.values()[secondaryDirectionIndex];
					int newSecondary = transform.rotate(secondaryDirection).ordinal();
					int newLineRotation = DirectionHelper.getCompressedSecondSide(newPrimary, newSecondary);
					long newLineFlag = 1L << (((newPrimary*4) + newLineRotation) + 6);
					newConnections |= newLineFlag;
				}
				
			}
		}
		// next 12 flags are edge flags, these are weirder but we have transform logic for them
		for (int edgeIndex=0; edgeIndex<12; edgeIndex++)
		{
			if ((oldConnections & (1L << (edgeIndex + 30))) != 0)
			{
				Edge oldEdge = Edge.values()[edgeIndex];
				Edge newEdge = oldEdge.transform(transform);
				long newEdgeFlag = 1L << (newEdge.ordinal() + 30);
				newConnections |= newEdgeFlag;
			}
		}
		return newConnections;
	}
}
