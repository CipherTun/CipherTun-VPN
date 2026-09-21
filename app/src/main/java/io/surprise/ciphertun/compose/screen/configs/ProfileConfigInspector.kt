package io.surprise.ciphertun.compose.screen.configs

import io.surprise.ciphertun.database.Profile
import org.json.JSONObject
import java.io.File

data class ProfileConfigSummary(
    val protocolType: String?,
    val serverAddress: String?,
)

private val NON_PROXY_TYPES = setOf(
    "direct", "block", "dns", "selector", "urltest", "tun",
    "mixed", "socks", "http", "redirect", "tproxy",
)

/**
 * Reads a profile's raw sing-box config off disk and pulls out the first
 * actual proxy outbound's protocol type + server:port, for display purposes
 * only (the NPV-style subtitle/tag on each config row). Never throws —
 * returns nulls on any parse failure so a malformed or not-yet-downloaded
 * remote config just shows a blank subtitle instead of crashing the list.
 */
object ProfileConfigInspector {
    fun summarize(profile: Profile): ProfileConfigSummary {
        return try {
            val file = File(profile.typed.path)
            if (!file.exists()) return ProfileConfigSummary(null, null)
            val root = JSONObject(file.readText())
            val outbounds = root.optJSONArray("outbounds") ?: return ProfileConfigSummary(null, null)
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                val type = outbound.optString("type", "")
                if (type.isEmpty() || NON_PROXY_TYPES.contains(type)) continue
                val server = outbound.optString("server", "")
                val port = outbound.optInt("server_port", -1)
                val address = if (server.isNotEmpty() && port > 0) "$server:$port" else null
                return ProfileConfigSummary(protocolType = type, serverAddress = address)
            }
            ProfileConfigSummary(null, null)
        } catch (_: Exception) {
            ProfileConfigSummary(null, null)
        }
    }
}
