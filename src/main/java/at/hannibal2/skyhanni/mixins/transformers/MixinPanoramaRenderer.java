package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.TitleScreenBackgroundHook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public class MixinPanoramaRenderer {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V", at = @At("HEAD"), cancellable = true)
    private void skyhanni$skipVanillaTitlePanoramaForNebula(GuiGraphics context, int width, int height, boolean rotate, CallbackInfo ci) {
        TitleScreenBackgroundHook.cancelVanillaPanoramaIfNebulaTitle(ci);
    }
}
