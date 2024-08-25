package twisb.exploration.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.LayeredDrawer;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twisb.exploration.CompassHud;

import java.util.function.Predicate;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    CompassHud compassHud;
    @Shadow
    LayeredDrawer layeredDrawer;
    @Shadow
    DebugHud debugHud;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void addCompassHud(MinecraftClient client, CallbackInfo ci) {
        this.compassHud = new CompassHud(client);
        LayeredDrawer compassDrawer = (new LayeredDrawer()).addLayer((context, tickCounter) -> {
            if (client.player != null) {
                Inventory inv = client.player.getInventory();
                boolean hasCompass = inv.containsAny(new Predicate<ItemStack>() {
                    public boolean test(ItemStack itemStack) {
                        return itemStack.isOf(Items.COMPASS);
                    }
                });
                if (!this.debugHud.shouldShowDebugHud() && hasCompass) {
                    this.compassHud.render(context);
                }
            }
        });
        this.layeredDrawer.addSubDrawer(compassDrawer, () -> {
            return !client.options.hudHidden;
        });
    }


}
