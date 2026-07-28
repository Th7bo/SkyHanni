package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.TitleScreenBackgroundHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {

    //~ if < 26.1 'extractBackground' -> 'renderBackground'
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), cancellable = true)
    private void skyhanni$titleNebulaBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onRenderBackgroundHead(context, ci);
    }

    @Inject(method = "init()V", at = @At("HEAD"))
    private void skyhanni$titleResetEntrance(CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onTitleScreenInit((TitleScreen) (Object) this);
    }

    //~ if < 26.1 'extractRenderState' -> 'render'
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("RETURN"))
    private void skyhanni$titleEntranceFade(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onRenderTail(context);
    }
}
