package commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;

import commoble.databuddy.nbt.NBTListCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class PostsInChunkCapability {
    /**
     * Don't get the default IPostsInChunk instance from this, it intentionally returns a broken instance that will
     * probably cause crashes if used
     **/
    public static Capability<IPostsInChunk> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

//	/** This codec serializes a list-like element **/
//	public static final Codec<Set<BlockPos>> POST_SET_CODEC = SetCodecHelper.makeSetCodec(BlockPos.CODEC);
//
//	public static class Storage implements Capability.IStorage<IPostsInChunk> {
//		public static final String POSITIONS = "positions";
//
//		@SuppressWarnings("deprecation")
//		private static final NBTListCodec<BlockPos, CompoundTag> POS_LISTER = new NBTListCodec<>(
//				POSITIONS,
//				NBTListCodec.ListTagType.COMPOUND,
//				NbtUtils::writeBlockPos,
//				NbtUtils::readBlockPos);
//
//		// this must return a CompoundNBT
//		@SuppressWarnings("deprecation")
//		public CompoundTag writeNBT(Capability<IPostsInChunk> capability, IPostsInChunk instance, Direction side) {
//			return POS_LISTER.write(new ArrayList<>(instance.getPositions()), new CompoundTag());
//		}
//
//		@SuppressWarnings("deprecation")
//		@Override
//		public void readNBT(Capability<IPostsInChunk> capability, IPostsInChunk instance, Direction side, Tag nbt) {
//			if (nbt instanceof CompoundTag) {
//				instance.setPositions(new HashSet<>(POS_LISTER.read((CompoundTag)nbt)));
//			}
//		}
//
//	}
}
