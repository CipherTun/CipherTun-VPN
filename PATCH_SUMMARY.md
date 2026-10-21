# CipherTun TCP proxy/payload and wizard layout patch — prepared from uploaded source

## What this patch adds
- Wizard form layout fix: the outer Scaffold's bottom inset is included in the scrollable form so Save & Connect remains reachable above the reserved banner slot even when an ad has not loaded.
- Shared HTTP CONNECT proxy and custom payload fields for Shadowsocks, VMess, VLESS, Trojan and SSH profiles, including split byte offset and inter-part delay.
- Profile/config serialization into common sing-box dialer options.
- A guarded build-time patcher that adds the fields to `option.DialerOptions` and wraps the shared TCP dialer with payload substitution, split/delay writes, HTTP CONNECT response validation, and buffered stream preservation.
- Go regression tests for placeholder expansion, CONNECT response/banner preservation, rejection of failed CONNECT responses, and split/delay writing.
- GitHub Actions integration that applies the patch to the resolved sing-box source and runs tests for the shared dialer plus SSH/Shadowsocks/VMess/VLESS/Trojan before the Android NDK install/build.

## Scope and limitations

This increment supports custom TCP payloads and HTTP CONNECT for Shadowsocks, VMess, VLESS, Trojan and SSH. It does not make the feature universal: UDP/QUIC-native protocols (Hysteria, Hysteria2, TUIC), WireGuard, OpenVPN/OpenConnect endpoints, and other non-TCP connection paths are not given these controls. It also does not implement arbitrary TLS wrapping, DNS tunneling, or every HTTP Injector carrier mode. A configured proxy must accept HTTP CONNECT; payloads and carrier behavior depend on the endpoint/network.

## Apply to your existing checkout
Extract the ZIP into the repository root so the included paths merge into place. Then inspect the diff and run the local checks below. The ZIP contains only changed/new source files; it deliberately excludes `.git`, generated outputs, signing files, and the rest of the project.

```sh
cd ~/storage/downloads/CipherTun-VPN || exit 1
git status --short --branch
unzip -o ~/storage/downloads/CipherTun-TCP-Proxy-Payload-UI-Patch-v3.zip -d .
git diff --check
git diff --stat
git diff -- .github/workflows/android-build.yml app/src/main/java/io/surprise/ciphertun/config/OutboundProfile.kt app/src/main/java/io/surprise/ciphertun/config/SingBoxConfigFactory.kt app/src/main/java/io/surprise/ciphertun/compose/wizard/WizardCredentialFormScreen.kt
```

Do not commit/push until the diff is reviewed and the GitHub Actions run passes. The upstream patcher intentionally fails if sing-box source anchors have changed.
