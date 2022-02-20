package commoble.morered;

import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class BlockAndBlockItem<B extends Block, I extends BlockItem> {

    public final Supplier<? extends B> blockGetter;
    public final Supplier<? extends I> itemGetter;

    public BlockAndBlockItem(Supplier<? extends B> blockGetter, Supplier<? extends I> itemGetter) {
        this.blockGetter = blockGetter;
        this.itemGetter = itemGetter;
    }

}
