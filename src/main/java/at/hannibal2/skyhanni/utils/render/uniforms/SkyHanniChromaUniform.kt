package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import net.minecraft.client.renderer.DynamicUniformStorage
import org.joml.Vector3f
import java.nio.ByteBuffer

class SkyHanniChromaUniform : AutoCloseable {
    private val uniformSize = Std140SizeCalculator()
        .putFloat().putFloat().putFloat().putInt().putInt().putVec3().putVec3().get()

    val storage = DynamicUniformStorage<UniformValue>("SkyHanni Chroma UBO", uniformSize, 2)

    fun writeWith(
        chromaSize: Float,
        timeOffset: Float,
        saturation: Float,
        forwardDirection: Int,
        useCustomColors: Int,
        customColor1: Vector3f,
        customColor2: Vector3f,
    ): GpuBufferSlice {
        return storage.writeUniform(
            UniformValue(chromaSize, timeOffset, saturation, forwardDirection, useCustomColors, customColor1, customColor2),
        )
    }

    // Imperative to clear DynamicUniformStorage every frame.
    // Handled in MixinRenderSystem.
    fun clear() {
        storage.endFrame()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val chromaSize: Float,
        val timeOffset: Float,
        val saturation: Float,
        val forwardDirection: Int,
        val useCustomColors: Int,
        val customColor1: Vector3f,
        val customColor2: Vector3f,
    ) : DynamicUniformStorage.DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(chromaSize)
                .putFloat(timeOffset)
                .putFloat(saturation)
                .putInt(forwardDirection)
                .putInt(useCustomColors)
                .putVec3(customColor1)
                .putVec3(customColor2)
        }
    }
}
