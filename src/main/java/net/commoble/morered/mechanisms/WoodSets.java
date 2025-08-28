package net.commoble.morered.mechanisms;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class WoodSets
{
	private WoodSets() {}
	
	public static final Map<String, WoodSet> LOOKUP = Util.make(new TreeMap<>(), map -> {
		map.put("acacia", new WoodSet(Blocks.STRIPPED_ACACIA_LOG, MapColor.COLOR_ORANGE, SoundType.WOOD, true));
		map.put("birch", new WoodSet(Blocks.STRIPPED_BIRCH_LOG, MapColor.SAND, SoundType.WOOD, true));
		map.put("cherry", new WoodSet(Blocks.STRIPPED_CHERRY_LOG, MapColor.TERRACOTTA_WHITE, SoundType.WOOD, true));
		map.put("crimson", new WoodSet(Blocks.STRIPPED_CRIMSON_STEM, MapColor.CRIMSON_STEM, SoundType.STEM, false));
		map.put("dark_oak", new WoodSet(Blocks.STRIPPED_DARK_OAK_LOG, MapColor.COLOR_BROWN, SoundType.WOOD, true));
		map.put("jungle", new WoodSet(Blocks.STRIPPED_JUNGLE_LOG, MapColor.DIRT, SoundType.WOOD, true));
		map.put("mangrove", new WoodSet(Blocks.STRIPPED_MANGROVE_LOG, MapColor.COLOR_RED, SoundType.WOOD, true));
		map.put("oak", new WoodSet(Blocks.STRIPPED_OAK_LOG, MapColor.WOOD, SoundType.WOOD, true));
		map.put("pale_oak", new WoodSet(Blocks.STRIPPED_PALE_OAK_LOG, MapColor.QUARTZ, SoundType.WOOD, true));
		map.put("spruce", new WoodSet(Blocks.STRIPPED_SPRUCE_LOG, MapColor.PODZOL, SoundType.WOOD, true));
		map.put("warped", new WoodSet(Blocks.STRIPPED_WARPED_STEM, MapColor.WARPED_STEM, SoundType.STEM, false));
	});
		
	public static record WoodSet(Block strippedLog, MapColor mapColor, SoundType soundType, boolean ignitedByLava)
	{
		public BlockBehaviour.Properties finagleProps(BlockBehaviour.Properties props)
		{
			if (this.ignitedByLava)
			{
				props.ignitedByLava();
			}
			return props;
		}
	}
}
