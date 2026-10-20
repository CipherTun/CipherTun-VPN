# Custom payloads and upstream proxies

CipherTun VPN uses sing-box as its tunnel engine. It can provide some proxy
and request-header controls found in apps such as HTTP Injector, HTTP Custom,
and DarkTunnel. Support depends on the selected protocol, transport, and
sing-box version. These controls are not a universal raw-payload engine.

The JSON example is a template, not a working server or a complete,
ready-to-connect VPN profile. Replace all placeholders and integrate the
outbounds into a profile with the required inbound and routing configuration.

## Feature mapping

| Feature | Closest sing-box option | Limitation |
| --- | --- | --- |
| Target host and port | Protocol outbound `server` / `server_port` | Other protocol fields are still required. |
| UUID or password | Protocol-specific `uuid` / `password` | Depends on the protocol. |
| TLS SNI | `tls.server_name` | TLS configuration, not an HTTP payload. |
| Upstream HTTP proxy | `http` outbound with supported `detour` chaining | Not every protocol/transport combination supports every chain. |
| HTTP proxy request headers | `headers` on the HTTP outbound | The core controls request construction, order, casing, and timing. |
| WebSocket Host and path | WebSocket `transport.path` and `transport.headers` | Applies to compatible WebSocket transports only. |
| TLS handshake fragmentation | `tls.fragment` / `tls.record_fragment`, where supported | Not arbitrary HTTP payload splitting. |

## HTTP CONNECT proxy chaining

The example shows a VLESS outbound using an HTTP outbound as its detour.
The HTTP outbound acts as a CONNECT proxy client. This is a proxy-to-target
chain, not arbitrary raw payload injection into a carrier gateway.

Consult the documentation for the exact sing-box version being built.
HTTP outbound fields and supported HTTP versions can vary by release.

## Features that require engine support

Ordinary sing-box JSON does not provide a universal HTTP Injector-style
payload template for every protocol. The following must not be exposed as
working features unless the selected engine actually implements them:

- `[crlf]`, `[split]`, and `[delay_split]` raw payload tokens.
- Front Inject / Back Inject sequences around a proxy handshake.
- Guaranteed byte-exact header order, capitalization, duplicates, or timing.
- One identical payload implementation for VLESS, VMess, Trojan, SSH,
  Shadowsocks, WireGuard, and all other protocols.
- DNSTT / SlowDNS or proprietary UDP tunneling created merely by HTTP headers.

These capabilities require an engine or transport that explicitly implements
them, potentially through a maintained sing-box/libbox modification. They
need tests of the actual bytes exchanged, not just a GUI field or saved JSON.

## Official references

- https://sing-box.sagernet.org/configuration/outbound/http/
- https://sing-box.sagernet.org/configuration/outbound/
- https://sing-box.sagernet.org/configuration/shared/tls/

Only use servers and networks you are authorized to access. Custom headers
or tunnels do not grant permission to bypass provider access controls,
billing rules, or terms.
