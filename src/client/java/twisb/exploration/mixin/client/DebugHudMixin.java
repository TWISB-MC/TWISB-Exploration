package twisb.exploration.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Shadow
    private MinecraftClient client;

    @Inject(method = "shouldShowDebugHud", at = @At("RETURN"), cancellable = true)
    public void debugIfCheatsEnabled(CallbackInfoReturnable<Boolean> cir) {
        boolean hasLevel = client.player != null && client.player.hasPermissionLevel(2);
        cir.setReturnValue(cir.getReturnValue() && hasLevel);
    }
}
