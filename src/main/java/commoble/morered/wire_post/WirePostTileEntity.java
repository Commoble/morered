package commoble.morered.wire_post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import commoble.databuddy.nbt.NBTListCodec;
import commoble.morered.MoreRed;
import commoble.morered.TileEntityRegistrar;
import commoble.morered.util.NestedBoundingBox;
import commoble.morered.util.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;

public class WirePostTileEntity extends TileEntity
{
	public static final String CONNECTIONS = "connections";
	public static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(0,0,0,0,0,0);

	private Map<BlockPos, NestedBoundingBox> remoteConnections = new HashMap<>();
	
	private AxisAlignedBB renderAABB = EMPTY_AABB; // used by client, updated whenever NBT is read

	@SuppressWarnings("deprecation")
	public static final NBTListCodec<BlockPos, CompoundNBT> BLOCKPOS_LISTER = new NBTListCodec<>(
		CONNECTIONS,
		NBTListCodec.ListNBTType.COMPOUND,
		NBTUtil::writeBlockPos,
		NBTUtil::readBlockPos);
	
	public WirePostTileEntity(TileEntityType<? extends WirePostTileEntity> type)
	{
		super(type);
	}
	
	public WirePostTileEntity()
	{
		this(TileEntityRegistrar.REDWIRE_POST.get());
	}

	public static Optional<WirePostTileEntity> getPost(IWorld world, BlockPos pos)
	{
		return WorldHelper.getTileEntityAt(WirePostTileEntity.class, world, pos);
	}

	// connects two post TEs
	// returns whether the attempt to add a connection was successful
	public static boolean addConnection(IWorld world, BlockPos posA, BlockPos posB)
	{
		// if two post TEs exist at the given locations, connect them and return true
		// otherwise return false
		return getPost(world, posA).flatMap(postA -> getPost(world, posB).map(postB -> addConnection(world, postA, postB))).orElse(false);
	}

	// returns true if attempt to add a connection was successful
	public static boolean addConnection(IWorld world, @Nonnull WirePostTileEntity postA, @Nonnull WirePostTileEntity postB)
	{
		postA.addConnection(postB.pos);
		postB.addConnection(postA.pos);
		return true;
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

	@Override
	public void remove()
	{
		this.clearRemoteConnections();
		super.remove();
	}

	// returns true if post TEs exist at the given locations and both have a
	// connection to the other
	public static boolean arePostsConnected(IWorld world, BlockPos posA, BlockPos posB)
	{
		return getPost(world, posA).flatMap(postA -> getPost(world, posB).map(postB -> postA.hasRemoteConnection(posB) && postB.hasRemoteConnection(posA)))
			.orElse(false);
	}

	public void clearRemoteConnections()
	{
		this.remoteConnections.keySet().forEach(otherPos -> getPost(this.world, otherPos).ifPresent(otherPost -> otherPost.removeConnection(this.pos)));
		this.remoteConnections = new HashMap<>();
		this.onCommonDataUpdated();
	}

	// removes any connection between two posts to each other
	// if only one post exists for some reason, or only one post has a
	// connection to the other,
	// it will still attempt to remove its connection
	public static void removeConnection(IWorld world, BlockPos posA, BlockPos posB)
	{
		getPost(world, posA).ifPresent(post -> post.removeConnection(posB));
		getPost(world, posB).ifPresent(post -> post.removeConnection(posA));
	}

	private void addConnection(BlockPos otherPos)
	{
		this.remoteConnections.put(otherPos.toImmutable(), this.getNestedBoundingBoxForConnectedPos(otherPos));
		this.world.neighborChanged(this.pos, this.getBlockState().getBlock(), otherPos);
		this.onCommonDataUpdated();
	}

	private void removeConnection(BlockPos otherPos)
	{
		this.remoteConnections.remove(otherPos);
		this.world.neighborChanged(this.pos, this.getBlockState().getBlock(), otherPos);
		if (!this.world.isRemote)
		{
			// only send one break packet when breaking two connections
			int thisY = this.pos.getY();
			int otherY = otherPos.getY();
			if (thisY < otherY || (thisY == otherY && this.pos.hashCode() < otherPos.hashCode())) 
				MoreRed.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> this.world.getChunkAt(this.pos)),
					new WireBreakPacket(getConnectionVector(this.pos), getConnectionVector(otherPos)));
		}
		this.onCommonDataUpdated();
	}
	
	public void notifyConnections()
	{
		this.getRemoteConnections()
			.forEach(connectionPos -> this.world.neighborChanged(connectionPos, this.getBlockState().getBlock(), this.pos));
//			world.notifyNeighborsOfStateExcept(neighborPos, this, dir);
			
	}
	
	public static Vector3d getConnectionVector(BlockPos pos)
	{
		return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return this.renderAABB;
	}
	
	public static AxisAlignedBB getAABBContainingAllBlockPos(BlockPos startPos, Set<BlockPos> theRest)
	{
		return theRest.stream()
			.map(AxisAlignedBB::new)
			.reduce(EMPTY_AABB, AxisAlignedBB::union, AxisAlignedBB::union)
			.union(new AxisAlignedBB(startPos));
	}

	public void onCommonDataUpdated()
	{
		this.markDirty();
		this.world.notifyBlockUpdate(this.pos, this.getBlockState(), this.getBlockState(), Constants.BlockFlags.DEFAULT);
	}

	@Override
	public void read(BlockState state, CompoundNBT compound)
	{
		super.read(state, compound);
		this.readCommonData(compound);
	}
	
	@SuppressWarnings("deprecation")
	protected void readCommonData(CompoundNBT compound)
	{
		if (compound.contains(CONNECTIONS))
		{
			List<BlockPos> positions = BLOCKPOS_LISTER.read(compound);
			Map<BlockPos, NestedBoundingBox> newMap = new HashMap<>();
			positions.forEach(otherPos -> newMap.put(otherPos, this.getNestedBoundingBoxForConnectedPos(otherPos)));
			this.remoteConnections = newMap;
		}
		this.renderAABB = getAABBContainingAllBlockPos(this.pos, this.remoteConnections.keySet());
	}

	@SuppressWarnings("deprecation")
	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		super.write(compound);
		BLOCKPOS_LISTER.write(Lists.newArrayList(this.remoteConnections.keySet()), compound);
		return compound;
	}

	@Override
	// called on server when client loads chunk with TE in it
	public CompoundNBT getUpdateTag()
	{
		return this.write(new CompoundNBT()); // supermethods of write() and getUpdateTag() both call writeInternal
	}

	@Override
	// generate packet on server to send to client
	public SUpdateTileEntityPacket getUpdatePacket()
	{
		return new SUpdateTileEntityPacket(this.pos, 1, this.write(new CompoundNBT()));
	}

	@Override
	// read packet on client
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
	{
		super.onDataPacket(net, pkt);
		this.readCommonData(pkt.getNbtCompound());
	}
	
	public NestedBoundingBox getNestedBoundingBoxForConnectedPos(BlockPos otherPos)
	{
		Vector3d thisVec = getConnectionVector(this.pos);
		Vector3d otherVec = getConnectionVector(otherPos);
		boolean otherHigher = otherVec.y > thisVec.y;
		Vector3d higherVec = otherHigher ? otherVec : thisVec;
		Vector3d lowerVec = otherHigher ? thisVec : otherVec;
		Vector3d[] points = SlackInterpolator.getInterpolatedPoints(lowerVec, higherVec);
		int segmentCount = points.length - 1;
		AxisAlignedBB[] boxes = new AxisAlignedBB[segmentCount];
		for (int i=0; i<segmentCount; i++)
		{
			boxes[i] = new AxisAlignedBB(points[i], points[i+1]);
		}
		return NestedBoundingBox.fromAABBs(boxes);
	}
}
