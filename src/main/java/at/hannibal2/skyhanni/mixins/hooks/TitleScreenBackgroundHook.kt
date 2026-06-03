package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.render.NebulaTitleBackgroundRenderer
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.TitleScreen
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object TitleScreenBackgroundHook {

    /** Replay the entrance fade once per fresh TitleScreen instance, but not on resize-driven re-init. */
    private var lastSeenInstance: TitleScreen? = null

    /**
     * The nebula [RenderPipeline][net.minecraft.client.renderer.RenderPipelines] resolves its shader source from
     * `ShaderManager` the first time it is compiled. On the very first launch the `LoadingOverlay` (Mojang splash)
     * renders *while the initial resource reload is still running*, so the shader sources are not loaded yet — compiling
     * the pipeline then fails ("Couldn't find source for ... shader (skyhanni:nebula_title)") and the GPU device caches
     * it as permanently invalid, leaving a black screen forever after. We therefore refuse to touch the pipeline until
     * the first [TitleScreen.init], which only happens once the initial reload (and thus shader loading) has completed.
     */
    private var initialResourcesLoaded = false

    /**
     * The title panorama is drawn from `PanoramaRenderer.render` (Mojmap; Yarn: RotatingCubeMapRenderer), not only from `TitleScreen.renderBackground`.
     * When the nebula is enabled we suppress the cube-map pass so cancelling the title background hook is visible.
     */
    @JvmStatic
    fun cancelVanillaPanoramaIfNebulaTitle(ci: CallbackInfo) {
        if (Minecraft.getInstance().screen !is TitleScreen) return
        if (!shouldActivateNebulaTitleBackground()) return
        ci.cancel()
    }

    fun shouldActivateNebulaTitleBackground(): Boolean {
        val title = SkyHanniMod.feature.gui.titleScreen
        if (!title.animatedNebulaBackground) return false
        if (deferNebulaForPackCoreInstalled(title.preservePackCoreTitleBackground)) return false
        return true
    }

    fun onRenderBackgroundHead(context: GuiGraphicsExtractor, ci: CallbackInfo) {
        if (!shouldActivateNebulaTitleBackground()) return

        DrawContextUtils.setContext(context)
        try {
            NebulaTitleBackgroundRenderer.render()
            ci.cancel()
        } finally {
            DrawContextUtils.clearContext()
        }
    }

    fun onRenderTail(context: GuiGraphicsExtractor) {
        if (!shouldActivateNebulaTitleBackground()) return

        DrawContextUtils.setContext(context)
        try {
            NebulaTitleBackgroundRenderer.renderEntranceOverlay()
        } finally {
            DrawContextUtils.clearContext()
        }
    }

    /**
     * Drawn at the very start of `LoadingOverlay.render`, so the Mojang brand fill draws on top of the nebula.
     * As the brand color crossfades during fade-out, the nebula behind it becomes visible — matching Phase 1
     * of `modern-mc.mp4`. We draw at full opacity here; the brand fill alpha is what produces the cross-fade.
     */
    fun onLoadingOverlayRenderHead(context: GuiGraphicsExtractor) {
        // Never compile the pipeline before shaders are loaded (see [initialResourcesLoaded]).
        if (!initialResourcesLoaded) return
        if (!shouldActivateNebulaTitleBackground()) return

        DrawContextUtils.setContext(context)
        try {
            NebulaTitleBackgroundRenderer.render()
        } finally {
            DrawContextUtils.clearContext()
        }
    }

    /**
     * Called from `TitleScreen.init`. Minecraft also re-runs `init` on window resize, so we identity-compare
     * the screen instance to avoid replaying the fade when only the framebuffer changed.
     */
    fun onTitleScreenInit(screen: TitleScreen) {
        // Reaching the title screen guarantees the initial resource reload (and shader loading) has finished.
        initialResourcesLoaded = true
        if (lastSeenInstance === screen) return
        lastSeenInstance = screen
        NebulaTitleBackgroundRenderer.resetEntranceClock()
    }

    /**
     * When Pack Core stays on Minecraft's vanilla [TitleScreen] (e.g. their "Use Vanilla Main Menu"),
     * its field [com.github.kd_gaming1.packcore.config.PackCoreConfig.useVanillaTitleScreen] is true —
     * we still draw SkyHanni's nebula regardless of preserve, since there is nothing left to defer to.
     */
    private fun deferNebulaForPackCoreInstalled(preservePackCoreTitleBackground: Boolean): Boolean {
        if (!preservePackCoreTitleBackground) return false
        if (!PlatformUtils.isModInstalled("packcore")) return false
        if (packCoreUsesVanillaTitleScreen()) return false
        return true
    }

    private fun packCoreUsesVanillaTitleScreen(): Boolean {
        return runCatching {
            Class.forName("com.github.kd_gaming1.packcore.config.PackCoreConfig")
                .getField("useVanillaTitleScreen")
                .getBoolean(null)
        }.getOrDefault(false)
    }
}
