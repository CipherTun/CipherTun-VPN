package io.surprise.ciphertun.config

import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigFactory {

    fun build(profile: OutboundProfile): String {
        val root = JSONObject()
        root.put("dns", buildDns())
        root.put("inbounds", buildInbounds())
        root.put("outbounds", buildOutbounds(profile))
        root.put("route", buildRoute())
        return root.toString(2)
    }

    private fun buildDns(): JSONObject {
        val server = JSONObject()
            .put("tag", "dns-remote")
            .put("address", "tls://8.8.8.8")

        return JSONObject()
            .put("servers", JSONArray().put(server))
            .put("final", "dns-remote")
    }

    private fun buildInbounds(): JSONArray {
        val tun = JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("address", JSONArray().put("172.19.0.1/30"))
            .put("auto_route", true)
            .put("strict_route", true)
            .put("stack", "gvisor")
            .put("sniff", true)

        return JSONArray().put(tun)
    }

    private fun buildOutbounds(profile: OutboundProfile): JSONArray {
        val proxy = buildOutboundJson(profile).put("tag", "proxy")
        val direct = JSONObject().put("type", "direct").put("tag", "direct")
        val block = JSONObject().put("type", "block").put("tag", "block")
        val dns = JSONObject().put("type", "dns").put("tag", "dns-out")
        return JSONArray().put(proxy).put(direct).put(block).put(dns)
    }

    private fun buildRoute(): JSONObject {
        val dnsRule = JSONObject().put("protocol", "dns").put("outbound", "dns-out")
        return JSONObject()
            .put("rules", JSONArray().put(dnsRule))
            .put("final", "proxy")
            .put("auto_detect_interface", true)
    }

    private fun buildOutboundJson(profile: OutboundProfile): JSONObject = when (profile) {
        is OutboundProfile.Shadowsocks -> JSONObject()
            .put("type", "shadowsocks")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("method", profile.method)
            .put("password", profile.password)

        is OutboundProfile.VMess -> JSONObject()
            .put("type", "vmess")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("uuid", profile.uuid)
            .put("alter_id", profile.alterId)
            .put("security", profile.security)
            .apply {
                putTransport(profile.transport)
                putTls(profile.tls)
            }

        is OutboundProfile.VLess -> JSONObject()
            .put("type", "vless")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("uuid", profile.uuid)
            .put("flow", profile.flow)
            .apply {
                putTransport(profile.transport)
                putTls(profile.tls)
            }

        is OutboundProfile.Trojan -> JSONObject()
            .put("type", "trojan")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("password", profile.password)
            .apply {
                putTransport(profile.transport)
                putTls(profile.tls)
            }

        is OutboundProfile.Hysteria -> JSONObject()
            .put("type", "hysteria")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("auth_str", profile.authString)
            .put("obfs", profile.obfs)
            .put("up_mbps", profile.upMbps)
            .put("down_mbps", profile.downMbps)
            .apply { putTls(profile.tls) }

        is OutboundProfile.Hysteria2 -> JSONObject()
            .put("type", "hysteria2")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("password", profile.password)
            .apply {
                if (profile.obfsPassword.isNotBlank()) {
                    put(
                        "obfs",
                        JSONObject()
                            .put("type", "salamander")
                            .put("password", profile.obfsPassword)
                    )
                }
                putTls(profile.tls)
            }

        is OutboundProfile.Tuic -> JSONObject()
            .put("type", "tuic")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("uuid", profile.uuid)
            .put("password", profile.password)
            .put("congestion_control", profile.congestionControl)
            .apply { putTls(profile.tls) }

        is OutboundProfile.WireGuard -> JSONObject()
            .put("type", "wireguard")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("local_address", JSONArray().put(profile.localAddress))
            .put("private_key", profile.privateKey)
            .put("peer_public_key", profile.peerPublicKey)
            .apply {
                if (profile.presharedKey.isNotBlank()) {
                    put("pre_shared_key", profile.presharedKey)
                }
            }
            .put("mtu", profile.mtu)

        is OutboundProfile.Socks -> JSONObject()
            .put("type", "socks")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .apply {
                if (profile.username.isNotBlank()) put("username", profile.username)
                if (profile.password.isNotBlank()) put("password", profile.password)
            }

        is OutboundProfile.Http -> JSONObject()
            .put("type", "http")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .apply {
                if (profile.username.isNotBlank()) put("username", profile.username)
                if (profile.password.isNotBlank()) put("password", profile.password)
            }
    }

    private fun JSONObject.putTls(tls: TlsConfig) {
        if (!tls.enabled) return
        val tlsJson = JSONObject()
            .put("enabled", true)
            .put("server_name", tls.serverName)
            .put("insecure", tls.insecure)

        if (tls.alpn.isNotEmpty()) {
            tlsJson.put("alpn", JSONArray(tls.alpn))
        }
        if (tls.utlsFingerprint.isNotBlank()) {
            tlsJson.put(
                "utls",
                JSONObject().put("enabled", true).put("fingerprint", tls.utlsFingerprint)
            )
        }
        if (tls.realityPublicKey.isNotBlank()) {
            tlsJson.put(
                "reality",
                JSONObject()
                    .put("enabled", true)
                    .put("public_key", tls.realityPublicKey)
                    .put("short_id", tls.realityShortId)
            )
        }
        put("tls", tlsJson)
    }

    private fun JSONObject.putTransport(transport: TransportConfig) {
        val transportJson = when (transport) {
            is TransportConfig.None -> return
            is TransportConfig.Ws -> JSONObject()
                .put("type", "ws")
                .put("path", transport.path)
                .apply {
                    if (transport.host.isNotBlank()) {
                        put("headers", JSONObject().put("Host", transport.host))
                    }
                }

            is TransportConfig.Grpc -> JSONObject()
                .put("type", "grpc")
                .put("service_name", transport.serviceName)

            is TransportConfig.Http -> JSONObject()
                .put("type", "http")
                .put("path", transport.path)
                .apply {
                    if (transport.host.isNotBlank()) {
                        put("host", JSONArray().put(transport.host))
                    }
                }
        }
        put("transport", transportJson)
    }
}
