package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.TitleScreenBackgroundHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class MixinLoadingOverlay {

    //~ if < 26.1 'extractRenderState' -> 'render'
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"))
    private void skyhanni$nebulaUnderMojangSplash(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onLoadingOverlayRenderHead(context);
    }
}
