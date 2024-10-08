package net.commoble.morered.api;

public enum SignalStrength
{
	WEAK,
	STRONG;
	
	public SignalStrength max(SignalStrength that)
	{
		return this == STRONG || that == STRONG
			? STRONG
			: WEAK;
	}
}
