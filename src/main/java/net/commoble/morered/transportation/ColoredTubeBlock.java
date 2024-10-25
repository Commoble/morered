package net.commoble.morered.transportation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class ColoredTubeBlock extends TubeBlock
{
	private DyeColor color;

	public ColoredTubeBlock(ResourceLocation textureLocation, DyeColor color, Properties properties)
	{
		super(textureLocation, properties);
		this.color = color;
	}
	
	@Override
	public boolean isTubeCompatible(TubeBlock tube)
	{
		if (tube instanceof ColoredTubeBlock)
		{
			return ((ColoredTubeBlock)tube).color.equals(this.color);
		}
		else
		{
			return true;
		}
	}
}
