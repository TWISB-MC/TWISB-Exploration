package twisb.exploration.mixin;

import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BundleContentsComponent.Builder.class)
public interface BundleContentsComponentInterface {

    @Accessor
    public List<ItemStack> getStacks();

    @Accessor("occupancy")
    public void setOccupancy(Fraction occupancy);
}
