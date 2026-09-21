package io.surprise.ciphertun.config

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ParsedShareLink(
    val profile: OutboundProfile,
    val name: String
)

object ShareLinkParser {

    private val directSchemes = setOf(
        "vless",
        "vmess",
        "trojan",
        "ss",
        "shadowsocks",
        "hysteria",
        "hysteria2",
        "hy2",
        "tuic",
        "ssh",
        "socks",
        "socks5",
        "http",
        "https",
        "shadowtls",
        "anytls",
        "sn",
        "snell"
    )

    fun isSupportedDirectLink(value: String): Boolean {
        val scheme = runCatching {
            Uri.parse(value.trim()).scheme?.lowercase()
        }.getOrNull()

        return scheme in directSchemes
    }

    fun parse(value: String): ParsedShareLink {
        val raw = value.trim()
        val uri = Uri.parse(raw)
        val scheme = uri.scheme?.lowercase()
            ?: error("Missing share-link scheme")

        return when (scheme) {
            "vless" -> parseVless(uri)
            "vmess" -> parseVmess(raw, uri)
            "trojan" -> parseTrojan(uri)
            "ss", "shadowsocks" -> parseShadowsocks(raw, uri)
            "hysteria" -> parseHysteria(uri)
            "hysteria2", "hy2" -> parseHysteria2(uri)
            "tuic" -> parseTuic(uri)
            "ssh" -> parseSsh(uri)
            "socks", "socks5" -> parseSocks(uri)
            "http", "https" -> parseHttp(uri)
            "shadowtls" -> parseShadowTls(uri)
            "anytls" -> parseAnyTls(uri)
            "sn", "snell" -> parseSnell(uri)
            else -> error("Unsupported share-link protocol: $scheme")
        }
    }

    private fun parseVless(uri: Uri): ParsedShareLink {
        val uuid = decode(uri.userInfo?.substringBefore(":").orEmpty())
        require(uuid.isNotBlank()) { "VLESS UUID is missing" }

        val server = requireServer(uri)
        val port = port(uri, 443)

        val query = Query(uri)

        val transport = transport(query)
        val security = query["security"]?.lowercase()
        val tlsEnabled = security == "tls" ||
            security == "reality" ||
            query["tls"] == "1" ||
            query["tls"] == "true"

        val tls = TlsConfig(
            enabled = tlsEnabled,
            serverName = query["sni"].orEmpty(),
            insecure = bool(query["allowInsecure"] ?: query["insecure"]),
            alpn = csv(query["alpn"]),
            utlsFingerprint = query["fp"].orEmpty(),
            realityPublicKey = if (security == "reality") query["pbk"].orEmpty() else "",
            realityShortId = if (security == "reality") query["sid"].orEmpty() else ""
        )

        return ParsedShareLink(
            OutboundProfile.VLess(
                remark = remark(uri, server),
                server = server,
                serverPort = port,
                uuid = uuid,
                flow = query["flow"]?.ifBlank { "none" } ?: "none",
                transport = transport,
                tls = tls
            ),
            remark(uri, server)
        )
    }

    private fun parseVmess(raw: String, uri: Uri): ParsedShareLink {
        val encoded = raw.substringAfter("://", "")
        val jsonText = decodeBase64(encoded)

        val json = JSONObject(jsonText)

        val server = json.optString("add")
            .ifBlank { json.optString("server") }

        require(server.isNotBlank()) { "VMess server is missing" }

        val port = json.optString("port").toIntOrNull()
            ?: 443

        val network = json.optString("net", "tcp").lowercase()
        val host = json.optString("host")
        val path = json.optString("path").ifBlank { "/" }

        val transport = when (network) {
            "ws" -> TransportConfig.Ws(
                path = path,
                host = host
            )

            "grpc" -> TransportConfig.Grpc(
                serviceName = json.optString("path")
            )

            "h2", "http" -> TransportConfig.Http(
                path = path,
                host = host
            )

            "httpupgrade" -> TransportConfig.HttpUpgrade(
                path = path,
                host = host
            )

            else -> TransportConfig.None
        }

        val tlsValue = json.optString("tls").lowercase()
        val tlsEnabled =
            tlsValue == "tls" ||
            tlsValue == "reality" ||
            tlsValue == "true" ||
            runCatching { json.optBoolean("tls", false) }.getOrDefault(false)

        val security = json.optString("scy")
            .ifBlank { json.optString("security", "auto") }

        val tls = TlsConfig(
            enabled = tlsEnabled,
            serverName = json.optString("sni")
                .ifBlank { json.optString("host") },
            insecure =
                json.optBoolean("allowInsecure", false) ||
                    json.optString("allowInsecure").equals("true", true) ||
                    json.optString("allowInsecure") == "1",
            alpn = csv(json.optString("alpn")),
            utlsFingerprint = json.optString("fp"),
            realityPublicKey = json.optString("pbk"),
            realityShortId = json.optString("sid")
        )

        val profileName = json.optString("ps")
            .ifBlank { server }

        return ParsedShareLink(
            OutboundProfile.VMess(
                remark = profileName,
                server = server,
                serverPort = port,
                uuid = json.optString("id"),
                alterId = json.optString("aid").toIntOrNull() ?: 0,
                security = security.ifBlank { "auto" },
                transport = transport,
                tls = tls
            ),
            profileName
        )
    }

    private fun parseTrojan(uri: Uri): ParsedShareLink {
        val password = decode(uri.userInfo.orEmpty())
        require(password.isNotBlank()) { "Trojan password is missing" }

        val server = requireServer(uri)
        val query = Query(uri)

        val tls = TlsConfig(
            enabled = true,
            serverName = query["sni"].orEmpty(),
            insecure = bool(query["allowInsecure"] ?: query["insecure"]),
            alpn = csv(query["alpn"]),
            utlsFingerprint = query["fp"].orEmpty()
        )

        return ParsedShareLink(
            OutboundProfile.Trojan(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                password = password,
                transport = transport(query),
                tls = tls
            ),
            remark(uri, server)
        )
    }

    private fun parseShadowsocks(raw: String, uri: Uri): ParsedShareLink {
        val fragmentName = fragment(uri)

        var server = uri.host
        var port = uri.port
        var method = ""
        var password = ""

        if (server == null) {
            val decoded = decodeBase64(
                raw.substringAfter("://")
                    .substringBefore("#")
            )

            val at = decoded.lastIndexOf("@")
            require(at > 0) { "Invalid Shadowsocks link" }

            val credentials = decoded.substring(0, at)
            val address = decoded.substring(at + 1)

            method = credentials.substringBefore(":")
            password = credentials.substringAfter(":", "")

            val addressUri = Uri.parse("ss://$address")
            server = addressUri.host
            port = addressUri.port
        } else {
            val userInfo = uri.userInfo.orEmpty()

            if (userInfo.isNotBlank()) {
                val decoded = decodeBase64Url(userInfo)
                if (decoded.contains(":")) {
                    method = decoded.substringBefore(":")
                    password = decoded.substringAfter(":")
                } else {
                    method = decode(userInfo.substringBefore(":"))
                    password = decode(userInfo.substringAfter(":", ""))
                }
            }

            if (method.isBlank()) {
                method = Query(uri)["method"].orEmpty()
            }

            if (password.isBlank()) {
                password = Query(uri)["password"].orEmpty()
            }
        }

        require(!server.isNullOrBlank()) { "Shadowsocks server is missing" }
        require(method.isNotBlank()) { "Shadowsocks method is missing" }

        val name = fragmentName ?: server

        return ParsedShareLink(
            OutboundProfile.Shadowsocks(
                remark = name,
                server = server,
                serverPort = if (port > 0) port else 8388,
                method = method,
                password = password
            ),
            name
        )
    }

    private fun parseHysteria(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val query = Query(uri)

        val auth = when {
            !uri.userInfo.isNullOrBlank() -> decode(uri.userInfo!!)
            !query["auth"].isNullOrBlank() -> query["auth"].orEmpty()
            else -> query["auth_str"].orEmpty()
        }

        val up = int(query["up_mbps"] ?: query["up"], 100)
        val down = int(query["down_mbps"] ?: query["down"], 100)

        val tls = TlsConfig(
            enabled = true,
            serverName = query["sni"].orEmpty(),
            insecure = bool(query["insecure"] ?: query["allowInsecure"]),
            alpn = csv(query["alpn"])
        )

        return ParsedShareLink(
            OutboundProfile.Hysteria(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                authString = auth,
                obfs = query["obfs"].orEmpty(),
                upMbps = up,
                downMbps = down,
                tls = tls
            ),
            remark(uri, server)
        )
    }

    private fun parseHysteria2(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val query = Query(uri)

        val password = decode(
            uri.userInfo
                ?: query["password"]
                ?: ""
        )

        val tls = TlsConfig(
            enabled = true,
            serverName = query["sni"].orEmpty(),
            insecure = bool(query["insecure"] ?: query["allowInsecure"]),
            alpn = csv(query["alpn"])
        )

        val obfsPassword =
            query["obfs-password"]
                ?: query["obfs_password"]
                ?: ""

        return ParsedShareLink(
            OutboundProfile.Hysteria2(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                password = password,
                obfsPassword = obfsPassword,
                upMbps = int(query["up_mbps"], 0),
                downMbps = int(query["down_mbps"], 0),
                tls = tls
            ),
            remark(uri, server)
        )
    }

    private fun parseTuic(uri: Uri): ParsedShareLink {
        val userInfo = uri.userInfo.orEmpty()
        val uuid = decode(userInfo.substringBefore(":"))
        val password = decode(userInfo.substringAfter(":", ""))

        require(uuid.isNotBlank()) { "TUIC UUID is missing" }

        val server = requireServer(uri)
        val query = Query(uri)

        val tls = TlsConfig(
            enabled = true,
            serverName = query["sni"].orEmpty(),
            insecure = bool(query["insecure"] ?: query["allowInsecure"]),
            alpn = csv(query["alpn"])
        )

        return ParsedShareLink(
            OutboundProfile.Tuic(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                uuid = uuid,
                password = password,
                congestionControl =
                    query["congestion_control"]
                        ?: query["congestion-control"]
                        ?: "bbr",
                tls = tls
            ),
            remark(uri, server)
        )
    }

    private fun parseSsh(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val query = Query(uri)
        val userInfo = uri.userInfo.orEmpty()

        val username = decode(userInfo.substringBefore(":"))
        val password = decode(userInfo.substringAfter(":", ""))

        return ParsedShareLink(
            OutboundProfile.Ssh(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 22),
                username = username,
                password = password,
                privateKey = decode(query["private_key"].orEmpty())
            ),
            remark(uri, server)
        )
    }

    private fun parseSocks(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val userInfo = uri.userInfo.orEmpty()

        val username = decode(userInfo.substringBefore(":"))
        val password = decode(userInfo.substringAfter(":", ""))

        return ParsedShareLink(
            OutboundProfile.Socks(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 1080),
                username = username,
                password = password
            ),
            remark(uri, server)
        )
    }

    private fun parseHttp(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val userInfo = uri.userInfo.orEmpty()

        val username =
            if (userInfo.isNotBlank()) {
                decode(userInfo.substringBefore(":"))
            } else {
                ""
            }

        val password =
            if (userInfo.contains(":")) {
                decode(userInfo.substringAfter(":", ""))
            } else {
                ""
            }

        return ParsedShareLink(
            OutboundProfile.Http(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, if (uri.scheme.equals("https", true)) 443 else 80),
                username = username,
                password = password,
                tls = TlsConfig(
                    enabled = uri.scheme.equals("https", true),
                    serverName = server
                )
            ),
            remark(uri, server)
        )
    }

    private fun parseShadowTls(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val query = Query(uri)

        return ParsedShareLink(
            OutboundProfile.ShadowTls(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                password = decode(uri.userInfo ?: query["password"].orEmpty()),
                version = int(query["version"], 3),
                tls = TlsConfig(
                    enabled = true,
                    serverName = query["sni"].orEmpty(),
                    insecure = bool(query["insecure"] ?: query["allowInsecure"]),
                    alpn = csv(query["alpn"])
                )
            ),
            remark(uri, server)
        )
    }

    private fun parseAnyTls(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val query = Query(uri)

        return ParsedShareLink(
            OutboundProfile.AnyTls(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                password = decode(uri.userInfo ?: query["password"].orEmpty()),
                tls = TlsConfig(
                    enabled = true,
                    serverName = query["sni"].orEmpty(),
                    insecure = bool(query["insecure"] ?: query["allowInsecure"]),
                    alpn = csv(query["alpn"])
                )
            ),
            remark(uri, server)
        )
    }

    private fun parseSnell(uri: Uri): ParsedShareLink {
        val server = requireServer(uri)
        val query = Query(uri)

        return ParsedShareLink(
            OutboundProfile.Snell(
                remark = remark(uri, server),
                server = server,
                serverPort = port(uri, 443),
                version = int(query["version"], 5),
                psk = decode(
                    uri.userInfo
                        ?: query["psk"]
                        ?: ""
                ),
                userkey = query["userkey"].orEmpty(),
                obfsMode = query["obfs_mode"]
                    ?: query["obfs-mode"]
                    ?: "none",
                obfsHost = query["obfs_host"]
                    ?: query["obfs-host"]
                    ?: "bing.com",
                mode = query["mode"] ?: "default"
            ),
            remark(uri, server)
        )
    }

    private fun transport(query: Query): TransportConfig {
        val type = (
            query["type"]
                ?: query["network"]
                ?: "tcp"
            ).lowercase()

        return when (type) {
            "ws", "websocket" -> TransportConfig.Ws(
                path = query["path"].orEmpty().ifBlank { "/" },
                host = query["host"].orEmpty()
            )

            "grpc" -> TransportConfig.Grpc(
                serviceName = query["serviceName"]
                    ?: query["service_name"]
                    ?: query["path"]
                    ?: ""
            )

            "h2", "http" -> TransportConfig.Http(
                path = query["path"].orEmpty().ifBlank { "/" },
                host = query["host"].orEmpty()
            )

            "httpupgrade" -> TransportConfig.HttpUpgrade(
                path = query["path"].orEmpty().ifBlank { "/" },
                host = query["host"].orEmpty()
            )

            else -> TransportConfig.None
        }
    }

    private fun requireServer(uri: Uri): String =
        uri.host?.takeIf { it.isNotBlank() }
            ?: error("Server address is missing")

    private fun port(uri: Uri, default: Int): Int =
        if (uri.port > 0) uri.port else default

    private fun remark(uri: Uri, fallback: String): String =
        fragment(uri) ?: fallback

    private fun fragment(uri: Uri): String? =
        uri.fragment
            ?.let { decode(it) }
            ?.takeIf { it.isNotBlank() }

    private fun csv(value: String?): List<String> =
        value.orEmpty()
            .split(",")
            .map { decode(it.trim()) }
            .filter { it.isNotBlank() }

    private fun bool(value: String?): Boolean =
        value.equals("1") ||
            value.equals("true", true) ||
            value.equals("yes", true)

    private fun int(value: String?, default: Int): Int =
        value?.toIntOrNull() ?: default

    private fun decode(value: String): String =
        runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }.getOrDefault(value)

    private fun decodeBase64(value: String): String {
        val clean = value
            .replace("-", "+")
            .replace("_", "/")
            .replace("\n", "")
            .replace("\r", "")

        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)

        return String(
            Base64.decode(padded, Base64.DEFAULT),
            StandardCharsets.UTF_8
        )
    }

    private fun decodeBase64Url(value: String): String {
        return String(
            Base64.decode(
                value,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            ),
            StandardCharsets.UTF_8
        )
    }

    private class Query(private val uri: Uri) {
        private val values = mutableMapOf<String, String>()

        init {
            uri.query?.split("&")?.forEach { item ->
                if (item.isBlank()) return@forEach

                val key = item.substringBefore("=", "")
                val value = item.substringAfter("=", "")

                if (key.isNotBlank()) {
                    values[decode(key)] = decode(value)
                }
            }
        }

        operator fun get(key: String): String? = values[key]
    }
}
