package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import net.minecraft.client.renderer.DynamicUniformStorage
import org.joml.Vector4f
import java.nio.ByteBuffer

class SkyHanniNebulaTitleUniform : AutoCloseable {

    private val uniformSize =
        Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get()

    val storage =
        DynamicUniformStorage<UniformValue>("SkyHanni Nebula Title UBO", uniformSize, 2)

    fun writeWith(
        framebufferWidth: Float,
        framebufferHeight: Float,
        timeSeconds: Float,
        shadowRgb: Vector4f,
        deepRgb: Vector4f,
        midRgb: Vector4f,
        brightRgb: Vector4f,
        hotRgb: Vector4f,
    ): GpuBufferSlice {
        return storage.writeUniform(
            UniformValue(
                framebufferWidth, framebufferHeight, timeSeconds,
                shadowRgb, deepRgb, midRgb, brightRgb, hotRgb,
            ),
        )
    }

    fun clear() {
        storage.endFrame()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val framebufferWidth: Float,
        val framebufferHeight: Float,
        val timeSeconds: Float,
        val shadowRgb: Vector4f,
        val deepRgb: Vector4f,
        val midRgb: Vector4f,
        val brightRgb: Vector4f,
        val hotRgb: Vector4f,
    ) : DynamicUniformStorage.DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(Vector4f(framebufferWidth, framebufferHeight, timeSeconds, 0f))
                .putVec4(shadowRgb)
                .putVec4(deepRgb)
                .putVec4(midRgb)
                .putVec4(brightRgb)
                .putVec4(hotRgb)
        }
    }
}
