# Custom payload and upstream proxy

CipherTun VPN builds on sing-box, so the "proxy" and "payload" fields found in
tunnel apps such as HTTP Custom, HTTP Injector and DarkTunnel can be expressed
as sing-box outbound options today, with no core changes.
The example in `docs/examples/vless-over-http-proxy.json` is an outbounds
fragment: paste the two outbounds into a profile and point `route.final` at
`vless-out`. All values in it are placeholders.

## Field mapping

| Tunnel app field | sing-box option |
| --- | --- |
| Target server `host:port` | `server` / `server_port` on the VLESS, VMess, Trojan, ... outbound |
| UUID / password | `uuid` / `password` |
| SNI | `tls.server_name` |
| Proxy `host:port` | an `http` outbound (`server` / `server_port`) used as the `detour` of the real outbound |
| Payload (CONNECT request with custom Host and headers) | `headers` on that `http` outbound |
| WebSocket Host and path | `transport.type = ws`, `transport.path`, `transport.headers` |
| TLS ClientHello splitting | `tls.fragment` / `tls.record_fragment` |

With `detour` set, the client connects to the proxy first, sends a
`CONNECT target:port` request through it, then runs TLS and the protocol to the
target inside that tunnel. This is the "Proxy -> Target" layout.

## How the HTTP outbound builds the request

Taken from the sing HTTP client source:

- A `Host` entry in `headers` becomes the `Host:` line of the `CONNECT`
  request, while the request line still names the real destination.
- Every other entry in `headers` (User-Agent and so on) is sent as given.
- `Proxy-Connection: Keep-Alive` is always added.
- `Host` and `path` cannot be combined.
- Header order and capitalisation are chosen by the client, not by you.

WebSocket example for a VLESS or Trojan outbound:

```json
"transport": {
  "type": "ws",
  "path": "/PATH",
  "headers": { "Host": "HOST_HEADER" }
}
```

Check the sing-box documentation for the exact field names of the core version
in `version.properties`.

## Not possible with config alone

- Raw payload templates with `[crlf]`, `[split]` or `[delay_split]`
- Front Inject and Back Inject (extra requests sent before or around the
  CONNECT)
- Byte-exact control of header order and casing
- DNSTT / SlowDNS and proprietary UDP modes

These need changes in the sing-box core, built into a custom libbox.

## Responsible use

Only use this with servers and networks you are allowed to use. Changing
request headers to get around a carrier's billing or access rules can breach
your contract or local law.
