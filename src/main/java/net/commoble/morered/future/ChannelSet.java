package net.commoble.morered.future;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.Util;
import net.minecraft.world.item.DyeColor;

public record ChannelSet(Collection<Channel> channels, Collection<Channel> compatibleChannels)
{
	public static final ChannelSet EMPTY = new ChannelSet(List.of(), List.of());
	public static final ChannelSet REDSTONE = new ChannelSet(List.of(Channel.wide()), Channel.ALL);
	public static final Map<DyeColor, ChannelSet> BY_COLOR = Util.make(new ImmutableMap.Builder<DyeColor, ChannelSet>(), builder -> {
		for (DyeColor color : DyeColor.values())
		{
			Channel channel = Channel.single(color);
			builder.put(color, new ChannelSet(List.of(Channel.single(color)), channel.expand()));
		}
	}).build();
	public static final ChannelSet ALL_COLORS = new ChannelSet(Channel.SIXTEEN_COLORS, Channel.ALL);
}
