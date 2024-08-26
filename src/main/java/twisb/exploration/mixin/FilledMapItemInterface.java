package twisb.exploration.mixin;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FilledMapItem.class)
public interface FilledMapItemInterface {

    @Invoker("scale")
    public static void invokeScale(ItemStack map, World world) {
        throw new AssertionError();
    }
}
