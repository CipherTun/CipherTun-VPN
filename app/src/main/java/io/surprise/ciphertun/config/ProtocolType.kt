package io.surprise.ciphertun.config

enum class ProtocolType(val displayName: String, val defaultPort: Int) {
    VLESS("VLESS", 443),
    VMESS("VMess", 443),
    TROJAN("Trojan", 443),
    SHADOWSOCKS("Shadowsocks", 8388),
    HYSTERIA2("Hysteria2", 443),
    HYSTERIA("Hysteria", 443),
    TUIC("TUIC", 443),
    WIREGUARD("WireGuard", 51820),
    SOCKS("SOCKS5", 1080),
    HTTP("HTTP(S) Proxy", 8080);

    companion object {
        val pickerOrder = listOf(
            VLESS, VMESS, TROJAN, SHADOWSOCKS,
            HYSTERIA2, HYSTERIA, TUIC, WIREGUARD,
            SOCKS, HTTP
        )
    }
}
