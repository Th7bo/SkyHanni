package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import com.mojang.blaze3d.platform.NativeImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.w3c.dom.Node
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadata
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reusable GIF video player that renders frames as a fullscreen overlay.
 *
 * Usage:
 *   val player = VideoPlayer("video/myreel.gif")   // path under assets/skyhanni/
 *   player.play()                                   // start from frame 0
 *   // in a GuiOnTopRenderEvent handler:
 *   player.renderFullscreen()
 *
 * Tips for best results:
 *   - Keep GIFs short (≤30 s) and at low resolution (≤480p) to limit RAM usage.
 *   - Convert videos with: ffmpeg -i input.mp4 -vf "fps=15,scale=360:-1" output.gif
 *   - GIFs are loaded once at startup; only the current frame is kept on the GPU.
 */
class VideoPlayer(
    private val assetPath: String,
    private val loop: Boolean = false,
) {
    private val textureId: Identifier = createResourceLocation(
        "skyhanni",
        "dynamic/video/${assetPath.replace("/", "_").replace(".", "_")}",
    )

    private var gpuTexture: DynamicTexture? = null
    private var gpuImage: NativeImage? = null

    private var frames: List<VideoFrame> = emptyList()
    private var totalDuration = Duration.ZERO
    private var playStartTime = SimpleTimeMark.farPast()
    private var lastRenderedFrameIndex = -1

    var isLoaded = false
        private set

    val isPlaying: Boolean
        get() = isLoaded &&
            frames.isNotEmpty() &&
            !playStartTime.isFarPast() &&
            (loop || playStartTime.passedSince() < totalDuration)

    private data class VideoFrame(val image: BufferedImage, val delay: Duration)

    init {
        with(SkyHanniMod) {
            CoroutineSettings("VideoPlayer-$assetPath", Duration.INFINITE).launchCoroutine {
                val loaded = withContext(Dispatchers.IO) { loadGifFrames(assetPath) }
                DelayedRun.runNextTick {
                    frames = loaded
                    totalDuration = frames.fold(Duration.ZERO) { acc, f -> acc + f.delay }
                    isLoaded = true
                }
            }
        }
    }

    fun play() {
        playStartTime = SimpleTimeMark.now()
        lastRenderedFrameIndex = -1
    }

    fun stop() {
        playStartTime = SimpleTimeMark.farPast()
    }

    /**
     * Call this from a [at.hannibal2.skyhanni.events.GuiRenderEvent.GuiOnTopRenderEvent] handler.
     * Advances to the correct frame for the current time, uploads it to the GPU if it changed,
     * then draws it fullscreen with the given [alpha].
     */
    fun renderFullscreen(alpha: Float = 1f) {
        if (!isPlaying) return
        val elapsed = computeElapsed()
        val index = getFrameIndexAt(elapsed)
        if (index != lastRenderedFrameIndex) {
            uploadFrame(frames[index].image)
            lastRenderedFrameIndex = index
        }
        GuiRenderUtils.drawTexturedRect(0f, 0f, textureId, alpha)
    }

    private fun computeElapsed(): Duration {
        val raw = playStartTime.passedSince()
        if (!loop || totalDuration <= Duration.ZERO) return raw
        return (raw.inWholeMilliseconds % totalDuration.inWholeMilliseconds).milliseconds
    }

    private fun getFrameIndexAt(elapsed: Duration): Int {
        var cumulative = Duration.ZERO
        for ((i, frame) in frames.withIndex()) {
            cumulative += frame.delay
            if (elapsed < cumulative) return i
        }
        return frames.size - 1
    }

    private fun uploadFrame(bImg: BufferedImage) {
        val existing = gpuImage
        val native: NativeImage
        if (existing != null && existing.width == bImg.width && existing.height == bImg.height) {
            native = existing
        } else {
            existing?.close()
            native = NativeImage(NativeImage.Format.RGBA, bImg.width, bImg.height, false)
            gpuImage = native
        }
        for (y in 0 until bImg.height) {
            for (x in 0 until bImg.width) {
                native.setPixel(x, y, bImg.getRGB(x, y))
            }
        }
        val tex = gpuTexture
        if (tex == null) {
            val newTex = DynamicTexture({ "skyhanni-video-$assetPath" }, native)
            Minecraft.getInstance().textureManager.register(textureId, newTex)
            gpuTexture = newTex
        } else {
            tex.upload()
        }
    }

    // ----- GIF loading -----

    private fun loadGifFrames(path: String): List<VideoFrame> {
        val readers = ImageIO.getImageReadersByFormatName("GIF")
        if (!readers.hasNext()) return emptyList()
        val reader = readers.next()
        val stream = VideoPlayer::class.java.classLoader
            .getResourceAsStream("assets/skyhanni/$path") ?: return emptyList()
        return try {
            reader.input = ImageIO.createImageInputStream(stream)
            val count = reader.getNumImages(true)
            if (count <= 0) return emptyList()
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g: Graphics2D = canvas.createGraphics()
            val result = (0 until count).mapNotNull { i ->
                val meta = reader.getImageMetadata(i)
                val frame = reader.read(i) ?: return@mapNotNull null
                val delay = extractDelay(meta)
                val layout = extractLayout(meta)
                g.drawImage(frame, layout.left, layout.top, null)
                val snapshot = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                snapshot.createGraphics().also {
                    it.drawImage(canvas, 0, 0, null)
                    it.dispose()
                }
                if (layout.disposal == "restoreToBackgroundColor") {
                    g.clearRect(layout.left, layout.top, frame.width, frame.height)
                }
                VideoFrame(snapshot, delay)
            }
            g.dispose()
            result
        } finally {
            reader.dispose()
            stream.close()
        }
    }

    private fun extractDelay(meta: IIOMetadata): Duration {
        val tree = meta.getAsTree("javax_imageio_gif_image_1.0") as? Node ?: return 100.milliseconds
        val children = tree.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeName == "GraphicControlExtension") {
                val cs = node.attributes.getNamedItem("delayTime")?.nodeValue?.toIntOrNull() ?: 10
                return maxOf(cs * 10L, 20L).milliseconds
            }
        }
        return 100.milliseconds
    }

    private data class FrameLayout(val left: Int, val top: Int, val disposal: String)

    private fun extractLayout(meta: IIOMetadata): FrameLayout {
        val tree = meta.getAsTree("javax_imageio_gif_image_1.0") as? Node
            ?: return FrameLayout(0, 0, "doNotDispose")
        val children = tree.childNodes
        var left = 0
        var top = 0
        var disposal = "doNotDispose"
        for (i in 0 until children.length) {
            val node = children.item(i)
            when (node.nodeName) {
                "ImageDescriptor" -> {
                    left = node.attributes.getNamedItem("imageLeftPosition")?.nodeValue?.toIntOrNull() ?: 0
                    top = node.attributes.getNamedItem("imageTopPosition")?.nodeValue?.toIntOrNull() ?: 0
                }
                "GraphicControlExtension" -> {
                    disposal = node.attributes.getNamedItem("disposalMethod")?.nodeValue ?: "doNotDispose"
                }
            }
        }
        return FrameLayout(left, top, disposal)
    }
}
