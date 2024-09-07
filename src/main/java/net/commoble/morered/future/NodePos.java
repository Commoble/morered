package net.commoble.morered.future;

public record NodePos(Face face, Channel channel)
{	
	public NodePos withChannel(Channel otherChannel)
	{
		return new NodePos(face, otherChannel);
	}
}
