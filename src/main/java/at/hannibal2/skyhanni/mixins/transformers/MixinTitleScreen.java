package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.mixins.hooks.TitleScreenBackgroundHook;
import at.hannibal2.skyhanni.utils.system.PlatformUtils;
import kotlin.Unit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {

    @Unique
    private static boolean skyhanni$hasInited = false;

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    private void onCreate(boolean doBackgroundFade, LogoRenderer logoRenderer, CallbackInfo ci) {
        if (!skyhanni$hasInited && PlatformUtils.isDevEnvironment()) {
            at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests.loadAllMixinClasses();
        }
        skyhanni$hasInited = true;
    }

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void skyhanni$titleNebulaBackground(GuiGraphics context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onRenderBackgroundHead(context, ci);
    }

    @Inject(method = "init()V", at = @At("HEAD"))
    private void skyhanni$titleResetEntrance(CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onTitleScreenInit((TitleScreen) (Object) this);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void skyhanni$titleEntranceFade(GuiGraphics context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenBackgroundHook.INSTANCE.onRenderTail(context);
    }
}
