package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.TitleScreenBackgroundHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//~ if < 26.1 'Panorama' -> 'PanoramaRenderer'
import net.minecraft.client.renderer.Panorama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//~ if < 26.1 'Panorama' -> 'PanoramaRenderer'
@Mixin(Panorama.class)
public class MixinPanoramaRenderer {

    //~ if < 26.1 'extractRenderState' -> 'render'
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZ)V", at = @At("HEAD"), cancellable = true)
    private void skyhanni$skipVanillaTitlePanoramaForNebula(GuiGraphicsExtractor context, int width, int height, boolean rotate, CallbackInfo ci) {
        TitleScreenBackgroundHook.cancelVanillaPanoramaIfNebulaTitle(ci);
    }
}
