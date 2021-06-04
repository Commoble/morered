package commoble.morered;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;

public class BlockAndBlockItem<B extends Block, I extends BlockItem>
{
	
	public final Supplier<? extends B> blockGetter;
	public final Supplier<? extends I> itemGetter;
	
	public BlockAndBlockItem(Supplier<? extends B> blockGetter, Supplier<? extends I> itemGetter)
	{
		this.blockGetter = blockGetter;
		this.itemGetter = itemGetter;
	}

}
