package commoble.morered.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

import commoble.morered.wire_post.PostsInChunk;
import commoble.morered.wires.AbstractWireBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

public class ClientProxy
{	
	private static final Set<BlockPos> NO_POSTS = ImmutableSet.of();
	
	// block positions are in absolute world coordinates, not local chunk coords
	private static Map<ChunkPos, Set<BlockPos>> clientPostsInChunk = new HashMap<>();
	
	public static void clear()
	{
		clientPostsInChunk = new HashMap<>();
	}
	
	public static void updatePostsInChunk(ChunkPos pos, Set<BlockPos> posts)
	{
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return;
		
		LevelChunk chunk = level.getChunk(pos.x, pos.z);
		if (chunk == null)
			return;
		
		chunk.getCapability(PostsInChunk.CAPABILITY).ifPresent(cap -> cap.setPositions(Set.copyOf(posts)));
	}
	
	@Nonnull
	public static Set<BlockPos> getPostsInChunk(ChunkPos pos)
	{
		return clientPostsInChunk.getOrDefault(pos, NO_POSTS);
	}

	public static void initializeAbstractWireBlockClient(AbstractWireBlock block, Consumer<IClientBlockExtensions> consumer)
	{
		consumer.accept(new IClientBlockExtensions() {

			@Override
			public boolean addHitEffects(BlockState state, Level Level, HitResult target, ParticleEngine manager)
			{
				// if we have no wires here, we have no shape, so return true to disable particles
				// (otherwise the particle manager crashes because it performs an unsupported operation on the empty shape)
				return block.getWireCount(state) == 0;
			}
			
		});
	}
}
