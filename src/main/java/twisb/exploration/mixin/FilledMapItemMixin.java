package twisb.exploration.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FilledMapItem.class)
public class FilledMapItemMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("twisb-exploration");

//    @Invoker("scale")
//    public static void invokeScale(ItemStack map, World world) {
//        throw new AssertionError();
//    }

//    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
//        ItemStack map = user.getStackInHand(hand);
//        MapIdComponent mapIdComponent = map.get(DataComponentTypes.MAP_ID);
//        MapState ms1 = FilledMapItem.getMapState(mapIdComponent, world);
////        // Both ways works, but problem seems client vs server
////        MapState ms2 = FilledMapItem.getMapState(mapIdComponent, user.getWorld());
////        if (world.isClient()) {
////            LOGGER.info("Client sees: " + ms1.dimension);
////        } else {
////            LOGGER.info("Server sees: " + ms1.dimension);
////        }
//
//        return TypedActionResult.pass(map);
//    }
}
