CipherTun VPN — Banner Visibility Fix

Replace these repository files with the matching files from this patch:
1. app/src/main/java/io/surprise/ciphertun/ads/CipherTunBanner.kt
2. app/src/main/java/io/surprise/ciphertun/compose/MainActivity.kt

Behavior change:
- Banner container height is 0 while loading or after load failure.
- Container expands to the adaptive banner height only after onAdLoaded().
- A failed request collapses the slot immediately while bounded retry remains enabled.
- The top system inset is applied only while the top banner is actually loaded.
- Existing banner ad unit IDs are not changed.

No commit or push is included. Build and test the APK in GitHub Actions before release.
