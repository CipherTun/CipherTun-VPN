# CipherTun TCP proxy and custom payload engine patch

Adds shared TCP-level options to sing-box dialer configuration and applies them to TCP-based Shadowsocks, VMess, VLESS, Trojan and SSH profiles. Options include an HTTP CONNECT proxy host/port, custom pre-handshake payload, byte-position split and inter-part delay. Payload placeholders: `[host]`, `[port]`, `[host_port]`, `[crlf]`, `[lf]`, `[cr]`, `%host%`, and `%port%`.

When a proxy host is configured, a non-empty payload must send an HTTP CONNECT request and the engine validates a successful HTTP 2xx response before the protocol handshake. Any bytes read beyond the response headers are preserved. Direct payload injection without a proxy is also available for TCP connections.

This is **not a universal feature for every VPN protocol**. It targets Shadowsocks, VMess, VLESS, Trojan and SSH because their upstream connections are TCP-based. UDP/QUIC-native protocols such as Hysteria, Hysteria2 and TUIC, and endpoint types such as WireGuard/OpenVPN, are intentionally not given these fields by the GUI. Their transport and handshake requirements differ. This patch does not implement arbitrary TLS wrapping, DNS tunneling, or all HTTP Injector-specific carrier modes.

The build workflow patcher fails closed if upstream source anchors change. GitHub Actions must validate the selected sing-box release and all Android ABIs; no full build is claimed as passing before that run succeeds.
