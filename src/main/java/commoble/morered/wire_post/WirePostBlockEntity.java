package commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.mojang.math.OctahedralGroup;

import commoble.morered.MoreRed;
import commoble.morered.util.EightGroup;
import commoble.morered.util.NBTListCodec;
import commoble.morered.util.NestedBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

public class WirePostBlockEntity extends BlockEntity
{
	public static final String CONNECTIONS = "connections";
	public static final AABB EMPTY_AABB = new AABB(0,0,0,0,0,0);

	// we keep this as absolute positions in-memory but write them to relative/normalized positions on save
	private Map<BlockPos, NestedBoundingBox> remoteConnections = new HashMap<>();
	
	private AABB renderAABB = EMPTY_AABB; // used by client, updated whenever NBT is read

	public static final NBTListCodec<BlockPos, CompoundTag> BLOCKPOS_LISTER = new NBTListCodec<>(
		CONNECTIONS,
		NBTListCodec.ListTagType.COMPOUND,
		NbtUtils::writeBlockPos,
		NbtUtils::readBlockPos);
	
	public WirePostBlockEntity(BlockEntityType<? extends WirePostBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public WirePostBlockEntity(BlockPos pos, BlockState state)
	{
		super(MoreRed.get().redwirePostBeType.get(), pos, state);
	}

	// connects two post TEs
	// returns whether the attempt to add a connection was successful
	public static boolean addConnection(LevelAccessor world, BlockPos posA, BlockPos posB)
	{
		// if two post TEs exist at the given locations, connect them and return true
		// otherwise return false
		if (world.getBlockEntity(posA) instanceof WirePostBlockEntity postA && world.getBlockEntity(posB) instanceof WirePostBlockEntity postB)
		{
			return addConnection(world, postA, postB);
		}
		return false;
	}

	// returns true if attempt to add a connection was successful
	public static boolean addConnection(LevelAccessor world, @Nonnull WirePostBlockEntity postA, @Nonnull WirePostBlockEntity postB)
	{
		postA.addConnection(postB.worldPosition);
		postB.addConnection(postA.worldPosition);
		return true;
	}
	
	public void setConnectionsRaw(Map<BlockPos, NestedBoundingBox> connections)
	{
		this.remoteConnections = connections;
	}
	
	public Set<BlockPos> getRemoteConnections()
	{
		return ImmutableSet.copyOf(this.remoteConnections.keySet());
	}
	
	public Map<BlockPos, NestedBoundingBox> getRemoteConnectionBoxes()
	{
		return this.remoteConnections;
	}

	public boolean hasRemoteConnection(BlockPos otherPos)
	{
		return this.remoteConnections.keySet().contains(otherPos);
	}

	// returns true if post TEs exist at the given locations and both have a
	// connection to the other
	public static boolean arePostsConnected(LevelAccessor level, BlockPos posA, BlockPos posB)
	{
		return level.getBlockEntity(posA) instanceof WirePostBlockEntity postA
			&& level.getBlockEntity(posB) instanceof WirePostBlockEntity postB
			&& postA.hasRemoteConnection(posB)
			&& postB.hasRemoteConnection(posA);
	}

	public void clearRemoteConnections()
	{
		for (BlockPos otherPos : this.remoteConnections.keySet())
		{
			if (this.level.getBlockEntity(otherPos) instanceof WirePostBlockEntity otherPost)
			{
				otherPost.removeConnection(this.worldPosition);
			}
		}
		this.remoteConnections = new HashMap<>();
		this.onCommonDataUpdated();
	}

	// removes any connection between two posts to each other
	// if only one post exists for some reason, or only one post has a
	// connection to the other,
	// it will still attempt to remove its connection
	public static void removeConnection(LevelAccessor world, BlockPos posA, BlockPos posB)
	{
		if (world.getBlockEntity(posA) instanceof WirePostBlockEntity postA)
		{
			postA.removeConnection(posB);
		}
		if (world.getBlockEntity(posB) instanceof WirePostBlockEntity postB)
		{
			postB.removeConnection(posA);
		}
	}

	private void addConnection(BlockPos otherPos)
	{
		this.remoteConnections.put(otherPos.immutable(), this.getNestedBoundingBoxForConnectedPos(otherPos));
		this.level.neighborChanged(this.worldPosition, this.getBlockState().getBlock(), otherPos);
		this.onCommonDataUpdated();
	}

	private void removeConnection(BlockPos otherPos)
	{
		this.remoteConnections.remove(otherPos);
		this.level.neighborChanged(this.worldPosition, this.getBlockState().getBlock(), otherPos);
		if (!this.level.isClientSide)
		{
			// only send one break packet when breaking two connections
			int thisY = this.worldPosition.getY();
			int otherY = otherPos.getY();
			if (thisY < otherY || (thisY == otherY && this.worldPosition.hashCode() < otherPos.hashCode())) 
				MoreRed.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> this.level.getChunkAt(this.worldPosition)),
					new WireBreakPacket(getConnectionVector(this.worldPosition), getConnectionVector(otherPos)));
		}
		this.onCommonDataUpdated();
	}
	
	public void notifyConnections()
	{
		this.getRemoteConnections()
			.forEach(connectionPos -> this.level.neighborChanged(connectionPos, this.getBlockState().getBlock(), this.worldPosition));
//			world.notifyNeighborsOfStateExcept(neighborPos, this, dir);
			
	}
	
	public static Vec3 getConnectionVector(BlockPos pos)
	{
		return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public AABB getRenderBoundingBox()
	{
		return this.renderAABB;
	}
	
	public static AABB getAABBContainingAllBlockPos(BlockPos startPos, Set<BlockPos> theRest)
	{
		return theRest.stream()
			.map(AABB::new)
			.reduce(EMPTY_AABB, AABB::minmax, AABB::minmax)
			.minmax(new AABB(startPos));
	}

	public void onCommonDataUpdated()
	{
		this.setChanged();
		this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
	}

	@Override
	public void load(CompoundTag compound)
	{
		super.load(compound);
		this.readCommonData(compound);
	}
	
	protected void readCommonData(CompoundTag compound)
	{
		if (compound.contains(CONNECTIONS))
		{
			List<BlockPos> normalizedPositions = BLOCKPOS_LISTER.read(compound);
			List<BlockPos> absolutePositions = new ArrayList<>();
			for (BlockPos normalPos : normalizedPositions)
			{
				absolutePositions.add(this.denormalizePos(normalPos));
			}
			Map<BlockPos, NestedBoundingBox> newMap = new HashMap<>();
			absolutePositions.forEach(otherPos -> newMap.put(otherPos, this.getNestedBoundingBoxForConnectedPos(otherPos)));
			this.remoteConnections = newMap;
		}
		this.renderAABB = getAABBContainingAllBlockPos(this.worldPosition, this.remoteConnections.keySet());
	}

	@Override
	public void saveAdditional(CompoundTag compound)
	{
		super.saveAdditional(compound);
		List<BlockPos> normalizedPositions = new ArrayList<>();
		for (BlockPos absolutePos : this.remoteConnections.keySet())
		{
			normalizedPositions.add(this.normalizePos(absolutePos));
		}
		BLOCKPOS_LISTER.write(normalizedPositions, compound);
	}
	
	/**
	 * {@return Relative position normalized by this block's rotation/mirror transform.}
	 * @param absolutePos BlockPos representing an absolute position in the level.
	 */
	public BlockPos normalizePos(BlockPos absolutePos)
	{
		BlockPos relativePos = absolutePos.subtract(this.getBlockPos());
		OctahedralGroup normalizer = this.getBlockState().getValue(WirePostBlock.TRANSFORM).inverse();
		BlockPos normalizedPos = EightGroup.transform(relativePos, normalizer);
		return normalizedPos;
	}
	
	/**
	 * {@return Absolute position in the level.}
	 * @param normalPos BlockPos relative to this block's position and transform.
	 */
	public BlockPos denormalizePos(BlockPos normalPos)
	{
		OctahedralGroup denormalizer = this.getBlockState().getValue(WirePostBlock.TRANSFORM);
		BlockPos relativePos = EightGroup.transform(normalPos, denormalizer);
		BlockPos absolutePos = relativePos.offset(this.getBlockPos());
		return absolutePos;
	}

	@Override
	// called on server when client loads chunk with TE in it
	public CompoundTag getUpdateTag()
	{
		CompoundTag compound = super.getUpdateTag();
		this.saveAdditional(compound);
		return compound;
	}

	@Override
	// generate packet on server to send to client
	// don't need to override onDataPacket because it just calls load()
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}
	
	public NestedBoundingBox getNestedBoundingBoxForConnectedPos(BlockPos otherPos)
	{
		Vec3 thisVec = getConnectionVector(this.worldPosition);
		Vec3 otherVec = getConnectionVector(otherPos);
		boolean otherHigher = otherVec.y > thisVec.y;
		Vec3 higherVec = otherHigher ? otherVec : thisVec;
		Vec3 lowerVec = otherHigher ? thisVec : otherVec;
		Vec3[] points = SlackInterpolator.getInterpolatedPoints(lowerVec, higherVec);
		int segmentCount = points.length - 1;
		AABB[] boxes = new AABB[segmentCount];
		for (int i=0; i<segmentCount; i++)
		{
			boxes[i] = new AABB(points[i], points[i+1]);
		}
		return NestedBoundingBox.fromAABBs(boxes);
	}
}
