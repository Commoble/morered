//package commoble.morered.bagofyurting;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import commoble.bagofyurting.api.BagOfYurtingAPI;
//import commoble.bagofyurting.api.BlockDataDeserializer;
//import commoble.bagofyurting.api.BlockDataSerializer;
//import commoble.bagofyurting.api.RotationUtil;
//import commoble.morered.TileEntityRegistrar;
//import commoble.morered.util.NestedBoundingBox;
//import commoble.morered.wire_post.SlackInterpolator;
//import commoble.morered.wire_post.WirePostTileEntity;
//import commoble.morered.wires.BundledCableTileEntity;
//import commoble.morered.wires.WireTileEntity;
//import net.minecraft.block.BlockState;
//import net.minecraft.nbt.CompoundNBT;
//import net.minecraft.tileentity.TileEntity;
//import net.minecraft.util.Direction;
//import net.minecraft.util.Rotation;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.world.World;
//
//public class BagOfYurtingProxy
//{
//	public static void addBagOfYurtingCompat()
//	{
//		BlockDataSerializer<WireTileEntity> wireSerializer = BagOfYurtingProxy::writeWire;
//		BlockDataDeserializer<WireTileEntity> wireDeserializer = BagOfYurtingProxy::readWire;
//		BlockDataSerializer<BundledCableTileEntity> cableSerializer = BagOfYurtingProxy::writeCable;
//		BlockDataDeserializer<BundledCableTileEntity> cableDeserializer = BagOfYurtingProxy::readCable;
//		BlockDataSerializer<WirePostTileEntity> postSerializer = BagOfYurtingProxy::writePost;
//		BlockDataDeserializer<WirePostTileEntity> postDeserializer = BagOfYurtingProxy::readPost;
//		BagOfYurtingAPI.registerBlockDataTransformer(TileEntityRegistrar.WIRE.get(), wireSerializer, wireDeserializer);
//		BagOfYurtingAPI.registerBlockDataTransformer(TileEntityRegistrar.COLORED_NETWORK_CABLE.get(), wireSerializer,
//		wireDeserializer);
//		BagOfYurtingAPI.registerBlockDataTransformer(TileEntityRegistrar.BUNDLED_NETWORK_CABLE.get(), cableSerializer,
//		cableDeserializer);
//		BagOfYurtingAPI.registerBlockDataTransformer(TileEntityRegistrar.REDWIRE_POST.get(), postSerializer,
//		postDeserializer);
//		BagOfYurtingAPI.registerBlockDataTransformer(TileEntityRegistrar.BUNDLED_CABLE_POST.get(), postSerializer,
//		postDeserializer);
//		BagOfYurtingAPI.registerBlockDataTransformer(TileEntityRegistrar.BUNDLED_CABLE_RELAY_PLATE.get(),
//		postSerializer, postDeserializer);
//
//	}
//
//	static void writeWire(WireTileEntity wire, CompoundNBT nbt, Rotation rotation, BlockPos minYurt, BlockPos maxYurt,
//	BlockPos origin, BlockPos newOffset)
//	{
//		// make a fake wire to make writing simpler
//		TileEntity fakeTE = wire.getType().create();
//		if (!(fakeTE instanceof WireTileEntity))
//			return;
//		WireTileEntity fakeWire = (WireTileEntity)fakeTE;
//		fakeWire.setLevelAndPosition(wire.getLevel(), wire.getBlockPos());
//		int[] transformedPower = new int[6];
//		for (Direction dir : Direction.values())
//		{
//			Direction newDir = rotation.rotate(dir);
//			transformedPower[newDir.ordinal()] = wire.getPower(dir.ordinal());
//		}
//		fakeWire.setPowerRaw(transformedPower);
//		fakeWire.save(nbt);
//	}
//
//	static void readWire(WireTileEntity wire, CompoundNBT input, World world, BlockPos pos, BlockState state, Rotation
//	rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
//	{
//		wire.load(state, input);
//		int[] detransformedPower = new int[6];
//		for (Direction dir : Direction.values())
//		{
//			Direction detransformedDir = rotation.rotate(dir);
//			detransformedPower[detransformedDir.ordinal()] = wire.getPower(dir.ordinal());
//		}
//		wire.setPowerRaw(detransformedPower);
//	}
//
//	static void writeCable(BundledCableTileEntity cable, CompoundNBT nbt, Rotation rotation, BlockPos minYurt,
//	BlockPos maxYurt, BlockPos origin, BlockPos newOffset)
//	{
//		// make a fake wire to make writing simpler
//		TileEntity fakeTE = cable.getType().create();
//		if (!(fakeTE instanceof BundledCableTileEntity))
//			return;
//		BundledCableTileEntity fakeCable = (BundledCableTileEntity)fakeTE;
//		fakeCable.setLevelAndPosition(cable.getLevel(), cable.getBlockPos());
//		byte[][] transformedPower = new byte[6][16];
//		for (Direction dir : Direction.values())
//		{
//			Direction newDir = rotation.rotate(dir);
//			transformedPower[newDir.ordinal()] = cable.getPowerChannels(dir.ordinal()).clone();
//		}
//		fakeCable.setPowerRaw(transformedPower);
//		fakeCable.save(nbt);
//	}
//
//	static void readCable(BundledCableTileEntity cable, CompoundNBT input, World world, BlockPos pos, BlockState
//	state, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
//	{
//		cable.load(state, input);
//		byte[][] detransformedPower = new byte[6][16];
//		for (Direction dir : Direction.values())
//		{
//			Direction detransformedDir = rotation.rotate(dir);
//			detransformedPower[detransformedDir.ordinal()] = cable.getPowerChannels(dir.ordinal());
//		}
//		cable.setPowerRaw(detransformedPower);
//	}
//
//	static void writePost(WirePostTileEntity post, CompoundNBT nbt, Rotation rotation, BlockPos minYurt, BlockPos
//	maxYurt, BlockPos origin, BlockPos newOffset)
//	{
//		World world = post.getLevel();
//		BlockPos pos = post.getBlockPos();
//
//		// make a fake post to make writing simpler
//		TileEntity fakeTE = post.getType().create();
//		if (!(fakeTE instanceof WirePostTileEntity))
//			return;
//		WirePostTileEntity fakePost = (WirePostTileEntity)fakeTE;
//
//		// convert and store remote connections
//		Map<BlockPos, NestedBoundingBox> connectionsToSave = new HashMap<>();
//		Set<BlockPos> connectionsToBreak = new HashSet<>();
//		post.getRemoteConnections().forEach(remotePos ->
//		{
//			if (isPosWithin(remotePos, minYurt, maxYurt)) // if we'll be yurting the post connected via this
//			connection too
//			{
//				BlockPos rotatedOffset = RotationUtil.transformBlockPos(rotation, remotePos, origin);
//				connectionsToSave.put(rotatedOffset, NestedBoundingBox.EMPTY);
//			}
//			else
//			{
//				// otherwise, make sure we destroy the connection cleanly
//				connectionsToBreak.add(remotePos);
//			}
//		});
//		connectionsToBreak.forEach(remotePos -> WirePostTileEntity.removeConnection(world, pos, remotePos));
//		fakePost.setConnectionsRaw(connectionsToSave);
//
//		fakePost.save(nbt);
//	}
//
//	static void readPost(WirePostTileEntity post, CompoundNBT input, World world, BlockPos pos, BlockState state,
//	Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
//	{
//		post.setLevelAndPosition(world, pos);
//
//		// untransform remote connections
//		if (input.contains(WirePostTileEntity.CONNECTIONS))
//		{
//			List<BlockPos> transformedPositions = WirePostTileEntity.BLOCKPOS_LISTER.read(input);
//			List<BlockPos> detransformedPositions = new ArrayList<>();
//			transformedPositions.forEach(transformedPos ->
//			{
//				BlockPos detransformedPosition = RotationUtil.untransformBlockPos(rotation, transformedPos, origin);
//				// validate connection in new position -- make sure there's no collisions
//				// check connection from the primary side so the raytraces are always the same
//				boolean canMaintainConnection = (world.getBlockEntity(detransformedPosition) instanceof
//				WirePostTileEntity) &&
//					(isPosLower(pos, detransformedPosition)
//					? canMaintainConnection(world, pos, detransformedPosition)
//					: canMaintainConnection(world, detransformedPosition, pos));
//				if (canMaintainConnection)
//				{
//					detransformedPositions.add(detransformedPosition);
//				}
//			});
//			WirePostTileEntity.BLOCKPOS_LISTER.write(detransformedPositions, input);
//		}
//
//		post.load(state, input);
//	}
//
//	/**
//	 * Returns true if a blockpos is within the box defined by the minimal and maximal corners
//	 * @param pos pos to check
//	 * @param min minimal corner
//	 * @param max maximal corner
//	 * @return true if a blockpos is within the box defined by min and max, false otherwise
//	 */
//	static boolean isPosWithin(BlockPos pos, BlockPos min, BlockPos max)
//	{
//		int x = pos.getX();
//		int y = pos.getY();
//		int z = pos.getZ();
//		return x >= min.getX() && y >= min.getY() && z >= min.getZ()
//			&& x <= max.getX() && y <= max.getY() && z <= max.getZ();
//	}
//
//	// lower pos takes precedence
//	static boolean isPosLower(BlockPos pos, BlockPos remotePos)
//	{
//		int posY = pos.getY();
//		int remoteY = remotePos.getY();
//		return posY == remoteY
//			? pos.hashCode() < remotePos.hashCode()
//			: posY < remoteY;
//	}
//
//	static boolean canMaintainConnection(World world, BlockPos lowerPos, BlockPos upperPos)
//	{
//		return SlackInterpolator.getWireRaytraceHit(lowerPos, upperPos, world) == null;
//	}
//}
