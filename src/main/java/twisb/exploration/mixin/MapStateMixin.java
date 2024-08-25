package twisb.exploration.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.function.Predicate;

@Mixin(MapState.class)
public abstract class MapStateMixin extends PersistentState {

    private static final Logger LOGGER = LoggerFactory.getLogger("twisb-exploration");

    /**
     * @author SunScript_
     * @reason Can't be arsed to deal with lambdas
     */
    @Overwrite
    private static Predicate<ItemStack> getEqualPredicate(ItemStack stack) {
        MapIdComponent mapIdComponent = (MapIdComponent)stack.get(DataComponentTypes.MAP_ID);
        return (other) -> {
            if (other == stack) {
                return true;
            } else {
                return Objects.equals(mapIdComponent, other.get(DataComponentTypes.MAP_ID));
            }
        };
    }

//    // Debug dimension mystery on world reload
//    @Inject(method = "fromNbt", at=@At("HEAD"))
//    private static void printDimOnLoad(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfoReturnable<MapState> cir) {
//        LOGGER.info(String.valueOf(nbt.get("dimension")));
//    }

//    @Inject(method="fromNbt",
//            at=@At(
//                value="TAIL"))
//    private static void printDimOnInit(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfoReturnable<MapState> cir, @Local RegistryKey<World> dim,  @Local MapState mapState) {
//        LOGGER.info(dim.toString());
//        LOGGER.info(mapState.dimension.toString());
//    }
}
