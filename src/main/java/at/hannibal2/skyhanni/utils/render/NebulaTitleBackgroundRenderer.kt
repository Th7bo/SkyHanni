package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniNebulaTitleUniform
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import org.joml.Matrix3x2f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

object NebulaTitleBackgroundRenderer {

    private const val ENTRANCE_FADE_SECONDS = 0.4f

    private val projectionMatrix = ProjectionMatrixBuffer(
        "SkyHanni Nebula Title",
        //? if < 26.1 {
        /*1000.0f,
        11000.0f,
        true,
        *///?}
    )

    private val nebulaUniform = SkyHanniNebulaTitleUniform()

    /** Elapsed animation time starts when the renderer first runs each session */
    private var startNanos: Long = 0L

    /** Wall-clock anchor for the per-instance title-screen entrance fade. 0 means "not started yet". */
    private var entranceStartNanos: Long = 0L

    fun clearUniforms() {
        nebulaUniform.clear()
    }

    fun resetAnimationClock() {
        startNanos = 0L
    }

    fun resetEntranceClock() {
        entranceStartNanos = 0L
    }

    /** 0 at start of fade, 1 once UI is fully visible. Smoothstep eased. */
    private fun entranceProgress(): Float {
        if (entranceStartNanos == 0L) entranceStartNanos = System.nanoTime()
        val elapsed = (System.nanoTime() - entranceStartNanos) / 1_000_000_000f
        if (elapsed >= ENTRANCE_FADE_SECONDS) return 1f
        val t = (elapsed / ENTRANCE_FADE_SECONDS).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * Draw the nebula on top of an already-rendered TitleScreen, fading out as the entrance progresses.
     * This produces a cross-fade where the UI appears to emerge from the nebula.
     */
    fun renderEntranceOverlay() {
        if (Minecraft.getInstance().level != null) return
        val progress = entranceProgress()
        if (progress >= 1f) return
        render(alpha = 1f - progress)
    }

    fun render(alpha: Float = 1f) {
        if (Minecraft.getInstance().level != null) return
        if (alpha <= 0f) return

        // FragCoord-space shader (modern-mc-background.html recreation) assumes an axis-aligned full-screen quad.
        // GuiGraphics.pose() is rarely identity on the title screen; baking it distorts coords vs uniform resolution.
        val matrices = Matrix3x2f()
        val window = Minecraft.getInstance().window
        val scaledW = window.width.toFloat() / window.guiScale.toFloat()
        val scaledH = window.height.toFloat() / window.guiScale.toFloat()
        if (startNanos == 0L) startNanos = System.nanoTime()
        val timeSeconds = (System.nanoTime() - startNanos) / 1_000_000_000f

        val framebufferW = window.width.toFloat()
        val framebufferH = window.height.toFloat()

        val tintAlpha = (alpha.coerceIn(0f, 1f) * 255f).toInt() and 0xFF
        val tintColor = (tintAlpha shl 24) or 0xFFFFFF

        with(RenderPipelineDrawer) {
            val pipeline = SkyHanniRenderPipeline.NEBULA_TITLE()
            val buffer = getBuffer(pipeline)
            quadVertices(scaledW, scaledH).forEach { (x, y) ->
                buffer.addVertexWith2DPose(matrices, x, y).apply {
                    setColor(tintColor)
                }
            }

            RenderSystem.backupProjectionMatrix()
            RenderSystem.setProjectionMatrix(
                //~ if < 26.1 'Matrix4f().setOrtho(0f, scaledW, scaledH, 0f, 1000f, 11000f)' -> 'scaledW, scaledH'
                projectionMatrix.getBuffer(Matrix4f().setOrtho(0f, scaledW, scaledH, 0f, 1000f, 11000f)),
                ProjectionType.ORTHOGRAPHIC,
            )
            val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                Matrix4f().setTranslation(0.0f, 0.0f, -11000.0f),
                Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                Vector3f(),
                Matrix4f()
            )
            val pal = SkyHanniMod.feature.gui.titleScreen.nebulaPalette
            val nebulaSlice = nebulaUniform.writeWith(
                framebufferW,
                framebufferH,
                timeSeconds,
                rgbUniformVector(pal.shadow),
                rgbUniformVector(pal.deep),
                rgbUniformVector(pal.mid),
                rgbUniformVector(pal.brightWisps),
                rgbUniformVector(pal.hotAccent),
            )

            draw(pipeline, buffer.buildOrThrow()) { pass ->
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("DynamicTransforms", dynamicTransforms)
                pass.setUniform("SkyHanniNebulaTitleUniforms", nebulaSlice)
            }

            RenderSystem.restoreProjectionMatrix()
        }
    }

    private fun rgbUniformVector(colour: ChromaColour): Vector4f {
        val c = colour.toColor()
        return Vector4f(c.red / 255f, c.green / 255f, c.blue / 255f, 0f)
    }

    private fun quadVertices(scaledW: Float, scaledH: Float): List<Pair<Float, Float>> = listOf(
        0f to 0f,
        0f to scaledH,
        scaledW to scaledH,
        scaledW to 0f,
    )
}
