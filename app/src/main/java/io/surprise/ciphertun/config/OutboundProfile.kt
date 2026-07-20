package io.surprise.ciphertun.config

data class TlsConfig(
    val enabled: Boolean = true,
    val serverName: String = "",
    val insecure: Boolean = false,
    val alpn: List<String> = emptyList(),
    val utlsFingerprint: String = "",
    val realityPublicKey: String = "",
    val realityShortId: String = ""
)

sealed class TransportConfig {
    data object None : TransportConfig()
    data class Ws(val path: String = "/", val host: String = "") : TransportConfig()
    data class Grpc(val serviceName: String = "") : TransportConfig()
    data class Http(val host: String = "", val path: String = "/") : TransportConfig()
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
        val mtu: Int = 1408
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
}
