package net.commoble.morered;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class CommonTags
{
	
	public static class Items
	{
		private static TagKey<Item> tag(String path)
		{
			return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
		}
		
		public static final TagKey<Item> WRENCHES = tag("tools/wrench");
	}
}
