package io.surprise.ciphertun.compose.screen.configs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

sealed class PingResult {
    data class Success(val millis: Long) : PingResult()
    data class Failure(val reason: String) : PingResult()
}

/**
 * A plain TCP connect-and-time test against a profile's server:port. This is
 * deliberately NOT a full proxy-protocol handshake (that would need spinning
 * up a real sing-box outbound per profile) — it only proves the host is
 * reachable on that port and reports round-trip time to open the socket.
 * Good enough for a "does this server respond" signal; not a guarantee the
 * proxy credentials themselves are valid.
 */
object ProfilePingTester {
    private const val TIMEOUT_MS = 5000

    suspend fun ping(host: String, port: Int): PingResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            }
            PingResult.Success(System.currentTimeMillis() - start)
        } catch (e: Exception) {
            PingResult.Failure(e.message ?: "unreachable")
        }
    }
}
