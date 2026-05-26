package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@AutoService(WaypointFormat::class)
class SkyblockerWaypointFormat : WaypointFormat {

    data class SkyblockerWaypoint(
        @Expose val pos: List<Int>,
        @Expose val name: String,
        @Expose val colorComponents: List<Double>,
        @Expose val alpha: Double = 0.5,
        @Expose val shouldRender: Boolean = true,
    )

    data class SkyblockerWaypointGroup(
        @Expose val name: String,
        @Expose val island: String,
        @Expose val waypoints: List<SkyblockerWaypoint>,
    )

    override fun load(string: String): Waypoints<SkyHanniWaypoint>? {
        if (!string.trim().startsWith(HEADER)) return null
        val json = decode(string.trim().removePrefix(HEADER)) ?: return null
        return try {
            val type = object : TypeToken<List<SkyblockerWaypointGroup>>() {}.type
            val groups: List<SkyblockerWaypointGroup> = ConfigManager.gson.fromJson(json, type)
            var index = 1
            val waypoints = groups.flatMap { group ->
                group.waypoints.map { wp ->
                    SkyHanniWaypoint(
                        LorenzVec(wp.pos[0].toDouble(), wp.pos[1].toDouble(), wp.pos[2].toDouble()),
                        index++,
                        mutableMapOf("name" to wp.name),
                    )
                }
            }
            Waypoints(waypoints.toMutableList())
        } catch (e: Exception) {
            ChatUtils.debug(e.stackTraceToString())
            null
        }
    }

    private fun decode(data: String): String? = try {
        val bytes = Base64.getDecoder().decode(data)
        GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        ChatUtils.debug(e.stackTraceToString())
        null
    }

    override fun canLoad(string: String): Boolean = string.trim().startsWith(HEADER)

    override fun export(waypoints: Waypoints<SkyHanniWaypoint>): String {
        val skyblockerWaypoints = waypoints.map { wp ->
            SkyblockerWaypoint(
                pos = listOf(wp.location.x.toInt(), wp.location.y.toInt(), wp.location.z.toInt()),
                name = wp.options["name"] ?: wp.number.toString(),
                colorComponents = listOf(0.0, 1.0, 0.0),
            )
        }
        val group = SkyblockerWaypointGroup(name = "SkyHanni", island = "", waypoints = skyblockerWaypoints)
        val json = ConfigManager.gson.toJson(listOf(group))
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(json.toByteArray()) }
        return HEADER + Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    override val name: String get() = "skyblocker"

    private companion object {
        const val HEADER = "[Skyblocker-Waypoint-Data-V1]"
    }
}
