package io.surprise.ciphertun.config

import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigFactory {

    private fun isEndpointProfile(profile: OutboundProfile): Boolean =
        profile is OutboundProfile.OpenVpnClient || profile is OutboundProfile.OpenConnectClient ||
            profile is OutboundProfile.WireGuard

    fun build(profile: OutboundProfile): String {
        val root = JSONObject()
        root.put("dns", buildDns(profile))
        root.put("inbounds", buildInbounds())
        // OpenVPN/OpenConnect are sing-box "endpoints", not "outbounds" --
        // a separate top-level config section. Routing treats endpoint tags
        // the same as outbound tags for `route.final`, so the rest of the
        // config (DNS, route rules) doesn't need to know which one it is.
        if (isEndpointProfile(profile)) {
            root.put("endpoints", JSONArray().put(buildEndpointJson(profile).put("tag", "proxy")))
            root.put("outbounds", buildOutbounds(profile = null))
        } else {
            root.put("outbounds", buildOutbounds(profile))
        }
        root.put("route", buildRoute(isEndpointProfile(profile)))
        return root.toString(2)
    }

    private fun buildDns(profile: OutboundProfile): JSONObject {
        // Keep the public DNS resolver as the fallback. For OpenVPN and
        // OpenConnect, also consume DNS resolvers/search domains pushed by
        // the VPN server and route only their preferred split-DNS domains
        // through the VPN endpoint.
        val remote = JSONObject()
            .put("tag", "dns-remote")
            .put("type", "tls")
            .put("server", "8.8.8.8")
            .put("server_port", 853)
            .put("detour", "direct")

        val servers = JSONArray().put(remote)
        val rules = JSONArray()

        when (profile) {
            is OutboundProfile.OpenVpnClient -> {
                servers.put(
                    JSONObject()
                        .put("tag", "vpn-dns")
                        .put("type", "openvpn")
                        .put("endpoint", "proxy")
                        .put("accept_default_resolvers", false)
                        .put("accept_search_domain", true)
                )
                rules.put(
                    JSONObject()
                        .put("preferred_by", "vpn-dns")
                        .put("action", "route")
                        .put("server", "vpn-dns")
                )
            }

            is OutboundProfile.OpenConnectClient -> {
                servers.put(
                    JSONObject()
                        .put("tag", "vpn-dns")
                        .put("type", "openconnect")
                        .put("endpoint", "proxy")
                        .put("accept_default_resolvers", false)
                        .put("accept_search_domain", true)
                )
                rules.put(
                    JSONObject()
                        .put("preferred_by", "vpn-dns")
                        .put("action", "route")
                        .put("server", "vpn-dns")
                )
            }

            else -> Unit
        }

        return JSONObject()
            .put("servers", servers)
            .put("rules", rules)
            .put("final", "dns-remote")
    }

    private fun buildInbounds(): JSONArray {
        val tun = JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("address", JSONArray().put("172.19.0.1/30"))
            .put("auto_route", true)
        // Inline "sniff" on the inbound was removed in 1.13.0 -- the
        // route-level "sniff" rule action in buildRoute() replaces it.

        return JSONArray().put(tun)
    }

    /**
     * @param profile null when the actual proxy is an endpoint (OpenVPN/
     * OpenConnect/WireGuard) instead of an outbound -- in that case this
     * just returns the "direct" outbound (always required as a fallback).
     */
    private fun buildOutbounds(profile: OutboundProfile?): JSONArray {
        // "block" and "dns" outbound types were removed in sing-box 1.13.0
        // (legacy special outbounds -> rule actions migration). "direct" is
        // still a real outbound type, that one's fine.
        val direct = JSONObject().put("type", "direct").put("tag", "direct")
        val array = JSONArray()
        if (profile != null) {
            array.put(buildOutboundJson(profile).put("tag", "proxy"))
        }
        return array.put(direct)
    }

    private fun buildRoute(endpointProfile: Boolean): JSONObject {
        val rules = JSONArray()
            .put(JSONObject().put("action", "sniff"))
            .put(
                JSONObject()
                    .put("protocol", "dns")
                    .put("action", "hijack-dns")
            )

        if (endpointProfile) {
            rules.put(
                JSONObject()
                    .put("preferred_by", "wireguard")
                    .put("action", "route")
                    .put("outbound", "direct")
            )
        }

        return JSONObject()
            .put("rules", rules)
            .put("final", if (endpointProfile) "direct" else "proxy")
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
            .apply {
                if (profile.flow.isNotBlank() && profile.flow != "none") put("flow", profile.flow)
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
            .put("up", "${profile.upMbps} Mbps")
            .put("down", "${profile.downMbps} Mbps")
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
                if (profile.upMbps > 0) put("up_mbps", profile.upMbps)
                if (profile.downMbps > 0) put("down_mbps", profile.downMbps)
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

        // WireGuard was an outbound type until sing-box 1.11.0 deprecated it
        // and 1.13.0 removed it entirely -- it's an endpoint now, same as
        // OpenVPN/OpenConnect. See buildEndpointJson() and the comment on
        // isEndpointProfile() above.
        is OutboundProfile.WireGuard ->
            error("WireGuard is an endpoint profile, not an outbound (removed in sing-box 1.13.0) -- use buildEndpointJson()")

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
                putTls(profile.tls)
            }

        is OutboundProfile.Ssh -> JSONObject()
            .put("type", "ssh")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("user", profile.username)
            .apply {
                if (profile.password.isNotBlank()) put("password", profile.password)
                if (profile.privateKey.isNotBlank()) put("private_key", profile.privateKey)
            }

        is OutboundProfile.ShadowTls -> JSONObject()
            .put("type", "shadowtls")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("password", profile.password)
            .put("version", profile.version)
            .apply { putTls(profile.tls) }

        is OutboundProfile.AnyTls -> JSONObject()
            .put("type", "anytls")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("password", profile.password)
            .apply { putTls(profile.tls) }

        is OutboundProfile.Tor -> JSONObject()
            .put("type", "tor")
            .apply {
                if (profile.dataDirectory.isNotBlank()) put("data_directory", profile.dataDirectory)
            }

        is OutboundProfile.Snell -> JSONObject()
            .put("type", "snell")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("version", profile.version)
            .put("psk", profile.psk)
            .apply {
                if (profile.userkey.isNotBlank()) put("userkey", profile.userkey)
                if (profile.version == 5) {
                    if (profile.obfsMode != "none") {
                        put("obfs_mode", profile.obfsMode)
                        put("obfs_host", profile.obfsHost)
                    }
                } else {
                    // version 6
                    if (profile.mode != "default") put("mode", profile.mode)
                }
            }

        // OpenVPN/OpenConnect are endpoints, not outbounds -- build() routes
        // them to buildEndpointJson() instead and never calls this function
        // for them. These branches exist only so this `when` stays
        // exhaustive; reaching them means that invariant broke somewhere.
        is OutboundProfile.OpenVpnClient, is OutboundProfile.OpenConnectClient ->
            error("${profile::class.simpleName} is an endpoint profile, not an outbound -- use buildEndpointJson()")
    }

    private fun buildEndpointJson(profile: OutboundProfile): JSONObject = when (profile) {
        is OutboundProfile.WireGuard -> JSONObject()
            .put("type", "wireguard")
            .put("address", JSONArray().put(profile.localAddress))
            .put("private_key", profile.privateKey)
            .put("mtu", profile.mtu)
            .put(
                "peers",
                JSONArray().put(
                    JSONObject()
                        .put("address", profile.server)
                        .put("port", profile.serverPort)
                        .put("public_key", profile.peerPublicKey)
                        .put(
                            "allowed_ips",
                            JSONArray(profile.allowedIps.split(",").map { it.trim() }.filter { it.isNotBlank() }),
                        )
                        .apply {
                            if (profile.presharedKey.isNotBlank()) {
                                put("pre_shared_key", profile.presharedKey)
                            }
                            if (profile.persistentKeepalive > 0) {
                                put("persistent_keepalive_interval", profile.persistentKeepalive)
                            }
                            if (profile.reserved.isNotBlank()) {
                                val reservedInts = profile.reserved.split(",")
                                    .mapNotNull { it.trim().toIntOrNull() }
                                if (reservedInts.isNotEmpty()) {
                                    put("reserved", JSONArray(reservedInts))
                                }
                            }
                        },
                ),
            )

        is OutboundProfile.OpenVpnClient -> JSONObject()
            .put("type", "openvpn-client")
            .put("server", profile.server)
            .put("server_port", profile.serverPort)
            .put("network", profile.network)
            .apply {
                if (profile.username.isNotBlank()) put("username", profile.username)
                if (profile.password.isNotBlank()) put("password", profile.password)
            }
            .put(
                "tls",
                JSONObject().apply {
                    if (profile.caCertificate.isNotBlank()) put("certificate", JSONArray(profile.caCertificate.lines().filter { it.isNotBlank() }))
                    if (profile.clientCertificate.isNotBlank()) put("client_certificate", JSONArray(profile.clientCertificate.lines().filter { it.isNotBlank() }))
                    if (profile.clientKey.isNotBlank()) put("client_key", JSONArray(profile.clientKey.lines().filter { it.isNotBlank() }))
                    if (profile.controlWrapType != "none" && profile.controlWrapKey.isNotBlank()) {
                        put(
                            "control_wrap",
                            JSONObject().apply {
                                put("type", profile.controlWrapType)
                                put("key", JSONArray(profile.controlWrapKey.lines().filter { it.isNotBlank() }))
                                if (profile.controlWrapType == "tls_auth" && profile.controlWrapDirection.isNotBlank()) {
                                    put("direction", profile.controlWrapDirection)
                                }
                            },
                        )
                    }
                    if (profile.redirectGateway) {
                        put("redirect_gateway", true)
                    }
                },
            )

        is OutboundProfile.OpenConnectClient -> JSONObject()
            .put("type", "openconnect")
            .put("server", profile.server)
            .put("flavor", profile.flavor)
            .apply {
                if (profile.username.isNotBlank()) put("username", profile.username)
                if (profile.password.isNotBlank()) put("password", profile.password)
                if (profile.authGroup.isNotBlank()) put("auth_group", profile.authGroup)
                if (profile.certificateAuthority.isNotBlank() || profile.clientCertificate.isNotBlank() || profile.insecure) {
                    put(
                        "tls",
                        JSONObject().apply {
                            if (profile.insecure) put("insecure", true)
                            if (profile.certificateAuthority.isNotBlank()) {
                                put("certificate_authority", JSONArray(profile.certificateAuthority.lines().filter { it.isNotBlank() }))
                            }
                            if (profile.clientCertificate.isNotBlank()) put("client_certificate", JSONArray(profile.clientCertificate.lines().filter { it.isNotBlank() }))
                            if (profile.clientKey.isNotBlank()) put("client_key", JSONArray(profile.clientKey.lines().filter { it.isNotBlank() }))
                        },
                    )
                }
            }

        else -> error("${profile::class.simpleName} is an outbound profile, not an endpoint -- use buildOutboundJson()")
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
        if (tls.pinnedCertSha256.isNotEmpty()) {
            tlsJson.put("certificate_public_key_sha256", JSONArray(tls.pinnedCertSha256))
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

            is TransportConfig.HttpUpgrade -> JSONObject()
                .put("type", "httpupgrade")
                .put("path", transport.path)
                .apply {
                    if (transport.host.isNotBlank()) {
                        put("host", transport.host)
                    }
                }
        }
        put("transport", transportJson)
    }
}
