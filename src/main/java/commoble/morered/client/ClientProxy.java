package commoble.morered.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class ClientProxy
{
	public static Optional<ClientProxy> makeClientProxy()
	{
		return Optional.of(new ClientProxy());
	}
	
	private static final Set<BlockPos> NO_POSTS = ImmutableSet.of();
	
	// block positions are in absolute world coordinates, not local chunk coords
	private Map<ChunkPos, Set<BlockPos>> postsInChunk = new HashMap<>();
	
	public void updatePostsInChunk(ChunkPos pos, Set<BlockPos> posts)
	{
		this.postsInChunk.put(pos, posts);
	}
	
	@Nonnull
	public Set<BlockPos> getPostsInChunk(ChunkPos pos)
	{
		return this.postsInChunk.getOrDefault(pos, NO_POSTS);
	}
}
