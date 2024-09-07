package net.commoble.morered.future;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.Util;
import net.minecraft.world.item.DyeColor;

public sealed interface Channel permits Channel.Wide, Channel.Single
{
	public abstract Set<Channel> expand();
	
	public static Channel wide()
	{
		return Wide.INSTANCE;
	}
	
	public static Channel single(DyeColor color)
	{
		return SIXTEEN_COLORS_BY_COLOR[color.ordinal()];
	}
	
	public static final Channel[] SIXTEEN_COLORS_BY_COLOR = Util.make(new Channel[16], channels -> {
		for (DyeColor color : DyeColor.values())
		{
			channels[color.ordinal()] = new Single(color);
		}
	});
	
	public static final Set<Channel> SIXTEEN_COLORS = Set.of(SIXTEEN_COLORS_BY_COLOR);
	public static final Set<Channel> ALL = Set.of(Util.make(new Channel[17], channels -> {
		for (DyeColor color : DyeColor.values())
		{
			channels[color.ordinal()] = SIXTEEN_COLORS_BY_COLOR[color.ordinal()];
		}
		channels[16] = Wide.INSTANCE;
	}));
	public static final List<Set<Channel>> EXPANDED_SINGLES = Util.make(() -> {
		List<Set<Channel>> channels = new ArrayList<>(); 
		for (int i=0; i<16; i++)
		{
			channels.add(Set.of(Wide.INSTANCE, SIXTEEN_COLORS_BY_COLOR[i]));
		}
		return channels;
	});
	
	public static enum Wide implements Channel
	{
		INSTANCE;
		
		public Set<Channel> expand()
		{
			return ALL;
		}
	}
	
	public static record Single(DyeColor color) implements Channel
	{
		public Set<Channel> expand()
		{
			return EXPANDED_SINGLES.get(color.ordinal());
		}
	}
}
