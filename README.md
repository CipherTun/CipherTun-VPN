# CipherTun VPN

![CipherTun VPN](art/banner.png)

CipherTun VPN is an Android VPN client built on top of the [sing-box](https://github.com/SagerNet/sing-box) proxy core. It routes your device's traffic through an encrypted tunnel using whichever protocol you configure, with no scripting or manual JSON editing required.

## How it works

CipherTun runs the sing-box core as a native library (compiled Go, bundled as an AAR) inside a foreground Android service. There are two ways it can capture traffic:

- **VPN mode (TUN)** — creates a virtual network interface via Android's `VpnService` API and routes all device traffic through it. This is the default and works without root.
- **System proxy mode** — configures a system-wide HTTP/SOCKS proxy instead of a full TUN interface, for cases where a full VPN isn't wanted or available.

Once traffic reaches the sing-box core, it's encrypted and forwarded to your configured outbound server using the selected protocol. A background command server inside the app streams live connection status, traffic counters, and logs back to the UI in real time, so the dashboard reflects what the core is actually doing rather than a simulated state.

For users on rooted devices, an optional privileged mode (via an Xposed module) allows additional low-level features such as a root network bridge and USB/IP device sharing.

## Setting up a connection — no code required

Tap **Add Configuration**, pick a protocol, and fill in a plain form:

- VLESS, VMess, Trojan
- Shadowsocks
- Hysteria, Hysteria2
- TUIC
- WireGuard
- AnyTLS, ShadowTLS
- SSH
- Tor
- SOCKS5, HTTP(S) proxy

Each protocol's form only asks for the fields that protocol actually needs (server, port, credentials, and TLS/transport options where relevant). The app builds a valid sing-box configuration from those fields automatically and validates it before saving — you never see or edit raw JSON.

Remote subscription URLs are also supported for importing a list of servers at once, with optional auto-update on an interval.

## Additional features

- Per-app proxy — include or exclude specific apps from the tunnel
- Rule-based routing — bypass LAN traffic, block ads, route by geosite/geoip rule-sets
- Custom DNS — including DNS over HTTPS/TLS and FakeIP
- Live traffic stats and connection inspection
- Crash and out-of-memory reporting for diagnosing issues after the fact
- Network diagnostic tools (STUN test, network quality test)
- Config backup and restore

## Requirements

- Android 5.0 (API 21) or newer
- VPN permission (granted via the standard Android system prompt) for TUN mode
- Notification permission (Android 13+) to show live connection status
- Location permission is only requested if a profile specifically uses Wi-Fi SSID/BSSID-based routing rules — it is not required otherwise, and the data is used solely for that routing decision, never transmitted anywhere

## Version

Current version: **1.14.0-alpha.45**
sing-box core: built against Go 1.25.11

This is a fork under active development — expect the alpha versioning to mean occasional rough edges.

## Building

This project builds entirely via GitHub Actions (see `.github/workflows/android-build.yml`), in two stages:

1. **`build-libbox`** — compiles the sing-box core itself (a separate Go project) into `libbox.aar`, including Tor support
2. **`debug`/`release`** — builds this app against that `.aar`

To build locally instead, you'd need to reproduce both stages yourself: a Go toolchain + `gomobile` for stage one, and JDK 17 + Android SDK with NDK 28.0.13004108 for stage two. Given the two-stage complexity, using GitHub Actions is strongly recommended over a local build.

A signed release build additionally requires a keystore and the signing properties described in `app/build.gradle.kts`.

## First-time GitHub setup

`app/release.keystore` and `third_party/termux-app` are intentionally excluded from git (see `.gitignore`). Before pushing:

1. Run `scripts/setup-third-party.sh` once locally so the terminal-emulator/terminal-view modules exist for local builds. The CI workflow fetches them itself, so this step is only needed for building outside GitHub Actions.
2. Base64-encode your keystore and add it as a repo secret named `RELEASE_KEYSTORE_BASE64`:
   ```
   base64 -w0 app/release.keystore
   ```
3. Create a `local.properties`-style file with `KEYSTORE_PASS`, `ALIAS_NAME`, and `ALIAS_PASS`, base64-encode it, and add it as a repo secret named `LOCAL_PROPERTIES`:
   ```
   printf "KEYSTORE_PASS=xxx\nALIAS_NAME=xxx\nALIAS_PASS=xxx\n" | base64 -w0
   ```
4. Add both under **Settings → Secrets and variables → Actions** on the GitHub repo.

`.github/workflows/android-build.yml` builds `libbox.aar`/`libbox-legacy.aar` from `SagerNet/sing-box` via `make lib_android`, then builds this app against them. The `SING_BOX_REF` env var at the top of that file pins which sing-box version/branch to build against — update it as needed.
