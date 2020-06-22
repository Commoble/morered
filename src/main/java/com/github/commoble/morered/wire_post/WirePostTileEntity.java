package com.github.commoble.morered.wire_post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.commoble.morered.MoreRed;
import com.github.commoble.morered.TileEntityRegistrar;
import com.github.commoble.morered.util.NBTListHelper;
import com.github.commoble.morered.util.NestedBoundingBox;
import com.github.commoble.morered.util.WorldHelper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
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

	public static NBTListHelper<BlockPos> BLOCKPOS_LISTER = new NBTListHelper<>(CONNECTIONS, pos -> NBTUtil.writeBlockPos(pos), nbt -> NBTUtil.readBlockPos(nbt));

	public WirePostTileEntity()
	{
		super(TileEntityRegistrar.REDWIRE_POST.get());
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
		this.onDataUpdated();
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
		this.remoteConnections.put(otherPos, this.getNestedBoundingBoxForConnectedPos(otherPos));
		this.world.neighborChanged(this.pos, this.getBlockState().getBlock(), otherPos);
		this.onDataUpdated();
	}

	private void removeConnection(BlockPos otherPos)
	{
		this.remoteConnections.remove(otherPos);
		this.world.neighborChanged(this.pos, this.getBlockState().getBlock(), otherPos);
		if (!this.world.isRemote)
		{
			MoreRed.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> this.world.getChunkAt(this.pos)),
				new WireBreakPacket(getConnectionVector(this.pos), getConnectionVector(otherPos)));
		}
		this.onDataUpdated();
	}
	
	public void notifyConnections()
	{
		this.getRemoteConnections()
			.forEach(connectionPos -> this.world.neighborChanged(connectionPos, this.getBlockState().getBlock(), this.pos));
//			world.notifyNeighborsOfStateExcept(neighborPos, this, dir);
			
	}
	
	public static Vec3d getConnectionVector(BlockPos pos)
	{
		return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
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

	public void onDataUpdated()
	{
		this.markDirty();
		this.world.notifyBlockUpdate(this.pos, this.getBlockState(), this.getBlockState(), Constants.BlockFlags.DEFAULT);
	}

	@Override
	public void read(CompoundNBT compound)
	{
		super.read(compound);
		if (compound.contains(CONNECTIONS))
		{
			List<BlockPos> positions = BLOCKPOS_LISTER.read(compound);
			Map<BlockPos, NestedBoundingBox> newMap = new HashMap<>();
			positions.forEach(otherPos -> newMap.put(otherPos, this.getNestedBoundingBoxForConnectedPos(otherPos)));
			this.remoteConnections = newMap;
		}
		this.renderAABB = getAABBContainingAllBlockPos(this.pos, this.remoteConnections.keySet());
	}

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
		this.read(pkt.getNbtCompound());
	}
	
	public NestedBoundingBox getNestedBoundingBoxForConnectedPos(BlockPos otherPos)
	{
		Vec3d thisVec = getConnectionVector(this.pos);
		Vec3d otherVec = getConnectionVector(otherPos);
		boolean otherHigher = otherVec.y > thisVec.y;
		Vec3d higherVec = otherHigher ? otherVec : thisVec;
		Vec3d lowerVec = otherHigher ? thisVec : otherVec;
		Vec3d[] points = SlackInterpolator.getInterpolatedPoints(lowerVec, higherVec);
		int segmentCount = points.length - 1;
		AxisAlignedBB[] boxes = new AxisAlignedBB[segmentCount];
		for (int i=0; i<segmentCount; i++)
		{
			boxes[i] = new AxisAlignedBB(points[i], points[i+1]);
		}
		return NestedBoundingBox.fromAABBs(boxes);
	}
	
	/**
	 * Checks if a placed block would intersect any of this block's connections.
	 * @param placePos The position the block is being placed at
	 * @param placeState The blockstate being placed
	 * @param checkedPostPositions The positions of wire posts that have already been checked.
	 * Any posts in this list that this post is connected to is also connected to this post, and this connection has been
	 * verified to not intersect the placed block, so we don't need to check again.
	 * @return A vec3d of the intersecting hit, or null if there was no intersecting hit
	 */
	@Nullable
	public Vec3d doesBlockStateIntersectConnection(BlockPos placePos, BlockState placeState, Set<BlockPos> checkedPostPositions)
	{
		for (Entry<BlockPos, NestedBoundingBox> entry : this.remoteConnections.entrySet())
		{
			BlockPos pos = entry.getKey();
			if (!checkedPostPositions.contains(pos))
			{
				Vec3d hit = doesBlockStateIntersectConnection(this.pos, pos, placePos, placeState, entry.getValue(), this.getWorld());
				if (hit != null)
				{
					return hit;
				}
			}
		}
		return null;
	}
	
	@Nullable
	public static Vec3d doesBlockStateIntersectConnection(BlockPos startPos, BlockPos endPos, BlockPos placePos, BlockState placeState, NestedBoundingBox box, World world)
	{
		VoxelShape shape = placeState.getCollisionShape(world, placePos);
		for (AxisAlignedBB aabb : shape.toBoundingBoxList())
		{
			if (box.intersects(aabb.offset(placePos)))
			{
				// if we confirm the AABB intersects, do a raytrace as well
				boolean lastPosIsHigher = startPos.getY() < endPos.getY();
				BlockPos upperPos = lastPosIsHigher ? endPos : startPos;
				BlockPos lowerPos = lastPosIsHigher ? startPos : endPos; 
				return SlackInterpolator.getWireRaytraceHit(lowerPos, upperPos, world);
			}
		}
		return null;
	}
}
