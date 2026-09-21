package io.surprise.ciphertun.config

data class TlsConfig(
    val enabled: Boolean = true,
    val serverName: String = "",
    val insecure: Boolean = false,
    val alpn: List<String> = emptyList(),
    val utlsFingerprint: String = "",
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val pinnedCertSha256: List<String> = emptyList()
)

sealed class TransportConfig {
    data object None : TransportConfig()
    data class Ws(val path: String = "/", val host: String = "") : TransportConfig()
    data class Grpc(val serviceName: String = "") : TransportConfig()
    data class Http(val host: String = "", val path: String = "/") : TransportConfig()
    data class HttpUpgrade(val path: String = "/", val host: String = "") : TransportConfig()
}

sealed class OutboundProfile {
    abstract val remark: String
    abstract val server: String
    abstract val serverPort: Int

    data class Shadowsocks(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.SHADOWSOCKS.defaultPort,
        val method: String = "2022-blake3-aes-128-gcm",
        val password: String = ""
    ) : OutboundProfile()

    data class VMess(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.VMESS.defaultPort,
        val uuid: String = "",
        val alterId: Int = 0,
        val security: String = "auto",
        val transport: TransportConfig = TransportConfig.None,
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class VLess(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.VLESS.defaultPort,
        val uuid: String = "",
        val flow: String = "none",
        val transport: TransportConfig = TransportConfig.None,
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class Trojan(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.TROJAN.defaultPort,
        val password: String = "",
        val transport: TransportConfig = TransportConfig.None,
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class Hysteria(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.HYSTERIA.defaultPort,
        val authString: String = "",
        val obfs: String = "",
        val upMbps: Int = 100,
        val downMbps: Int = 100,
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class Hysteria2(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.HYSTERIA2.defaultPort,
        val password: String = "",
        val obfsPassword: String = "",
        val upMbps: Int = 0,
        val downMbps: Int = 0,
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class Tuic(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.TUIC.defaultPort,
        val uuid: String = "",
        val password: String = "",
        val congestionControl: String = "bbr",
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class WireGuard(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.WIREGUARD.defaultPort,
        val privateKey: String = "",
        val peerPublicKey: String = "",
        val presharedKey: String = "",
        val localAddress: String = "10.0.0.2/32",
        val allowedIps: String = "0.0.0.0/0,::/0",
        val mtu: Int = 1408,
        val reserved: String = ""
    ) : OutboundProfile()

    data class Socks(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.SOCKS.defaultPort,
        val username: String = "",
        val password: String = ""
    ) : OutboundProfile()

    data class Http(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.HTTP.defaultPort,
        val username: String = "",
        val password: String = ""
    ) : OutboundProfile()

    data class Ssh(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.SSH.defaultPort,
        val username: String = "",
        val password: String = "",
        val privateKey: String = ""
    ) : OutboundProfile()

    data class ShadowTls(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.SHADOWTLS.defaultPort,
        val password: String = "",
        val version: Int = 3,
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class AnyTls(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.ANYTLS.defaultPort,
        val password: String = "",
        val tls: TlsConfig = TlsConfig()
    ) : OutboundProfile()

    data class Tor(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = 0,
        val dataDirectory: String = ""
    ) : OutboundProfile()

    data class Snell(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = ProtocolType.SNELL.defaultPort,
        val version: Int = 4,
        val psk: String = "",
        val userkey: String = "",
        // v4 only
        val obfsMode: String = "none",
        val obfsHost: String = "bing.com",
        // v6 only
        val mode: String = "default"
    ) : OutboundProfile()

    // sing-box models this as an "endpoint", not an "outbound" -- see
    // SingBoxConfigFactory.build() for how that's reconciled. Scoped to the
    // fields that cover real .ovpn imports: TLS-mode auth (user/pass or
    // cert), plus the tls-auth/tls-crypt wrapper almost every .ovpn ships
    // with. Skips static-key mode (legacy/rare) and the ~30 tuning fields
    // (MSS, fragment, replay window, compression, renegotiation, routes)
    // that sing-box has sensible defaults for.
    data class OpenVpnClient(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = 1194,
        val network: String = "udp",
        val username: String = "",
        val password: String = "",
        val caCertificate: String = "",
        val clientCertificate: String = "",
        val clientKey: String = "",
        val controlWrapType: String = "none",
        val controlWrapKey: String = "",
        val controlWrapDirection: String = ""
    ) : OutboundProfile()

    // Also an "endpoint", not an "outbound". Scoped to what covers real
    // Cisco AnyConnect / GlobalProtect / Fortinet / F5 / Pulse / Juniper NC
    // gateways: flavor + username/password + optional CA/client cert. Skips
    // 2FA token modes, CSD/HIP/TNCC compliance wrappers, MCA certs, and
    // per-field form overrides -- all genuinely enterprise-NAC edge cases.
    data class OpenConnectClient(
        override val remark: String = "",
        override val server: String = "",
        override val serverPort: Int = 443,
        val flavor: String = "anyconnect",
        val username: String = "",
        val password: String = "",
        val authGroup: String = "",
        val insecure: Boolean = false,
        val certificateAuthority: String = "",
        val clientCertificate: String = "",
        val clientKey: String = ""
    ) : OutboundProfile()
}
