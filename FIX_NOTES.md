# Fix notes — merge of original repo + verified fixes

Base: your original repo export (fewer bugs than the "fixed" zip).
Everything below was changed on top of that base. Nothing else was touched.

## Brought over from the "fixed" zip (verified legit)
- `bg/LogEntry.java`, `NeighborEntry.java`, `PackageEntry.java`, `ParceledListSlice.java`
  — repackaged `io.nekohasekai.sfa.bg` → `io.surprise.ciphertun.bg`
- `SFANavigation.kt` — dropped the `sshSharedViewModel` argument passed to
  `ToolsScreen(...)`, which doesn't accept that parameter
- `third_party/libxposed-api/build.gradle.kts` — `androidResources { enable = false }`
  block syntax (newer AGP DSL)
- `scripts/setup-third-party.sh` — proguard-rules patch needed for AGP 9.4
- `.github/workflows/android-build.yml` — caching, secret validation, toolchain
  verification steps (branch trigger corrected back to `main`, see below)

## New regression from "fixed" that was fixed here instead
- `SFANavigation.kt:289` — was `usbIPViewModel ?: viewModel()` (self-reference,
  unresolved). Changed to `usbIPStatusViewModel ?: viewModel()`, matching the
  correct pattern already used at lines 323/340 in the same file.

## Rejected from "fixed" (verified wrong via web search)
- `detekt-formatting` coordinate — kept `io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8`
  (real group). "fixed" had `io.gitlab.detekt:...`, which isn't a published group.
- Xposed API repo — kept `https://api.xposed.info/` (real, current repo).
  "fixed" had `https://artifactory.appodeal.com/appodeal-public/`, an ad-SDK
  company's repo unrelated to Xposed.
- `org.jetbrains.kotlin.android` plugin — kept applied. "fixed" removed it
  entirely (relying on AGP's unproven/alpha built-in-Kotlin support). Official
  AGP 9.x docs still show this plugin applied explicitly.
- Workflow branch trigger — kept/restored to `main`. "fixed" had `dev`, but
  the live GitHub repo's default branch is currently `main`.

## Newly written (previously missing entirely in both zips)
- `compat/ComposeCompat.kt` — added `LazyColumnCompat` (thin `LazyColumn`
  pass-through) and `animateItemCompat` (standalone placement-animation
  `Modifier` for the manual drag-reorder list in `DashboardSettingsBottomSheet.kt`,
  which isn't inside a `LazyItemScope` so the real `animateItem()` doesn't apply)
- `vendor/PackageQueryManager.kt` — new file. Dispatches installed-package
  queries to root (via the existing `RootClient`) or Shizuku (via a
  `ShizukuBinderWrapper`-wrapped `IPackageManager`, same reflection technique
  as `PrivilegedServiceUtils`), based on `Settings.perAppProxyPackageQueryMode`,
  falling back to the normal filtered `PackageManager` query.

## Fixed package declarations
- `ManualScrollTextProcessor.java`: `io.nekohasekai.sfa...` → `io.surprise.ciphertun...`
- `TailscaleSSHTerminalSession.java`: `io.nekohasekai.sfa.terminal` → `io.surprise.ciphertun.terminal`

## Version bumps (AGP 9.4 as requested)
- AGP: `9.0.1` → `9.4.0-alpha06` (currently the newest available; AGP 9.4 has
  not reached stable yet — only alpha01–06 exist as of today)
- Gradle wrapper: `9.3.1` → `9.6.0` (AGP 9.4's documented minimum/default)
- NDK: `28.0.13004108` → `28.2.13676358` (AGP 9.4's documented default),
  updated in both `app/build.gradle.kts` and `gradle.properties`
- Kotlin/KSP left untouched at the original's `2.3.10` / `2.3.5` — no version
  bump was required for AGP 9.4, and "fixed"'s bump to `2.4.10` paired with
  KSP still at `2.3.10` looked like a version mismatch, not a real fix.

## Checked, found already correct, left untouched
- `.gitignore` — identical between original and "fixed", correctly ignores
  `app/release.keystore`, `app/schemas/`, build outputs, etc.
- `settings.gradle.kts` repository block — otherwise unchanged

## ⚠️ Could not verify (no network / no Android SDK in this environment)
None of this was run through an actual Gradle build — there's no network
access or Android SDK available here. `PackageQueryManager.kt` and the
`ComposeCompat.kt` additions are written against APIs I'm confident about
(Compose Foundation's `LazyColumn`/`overscrollEffect`, and the standard
Shizuku `rikka.shizuku:api` integration pattern used across many open-source
apps), but they're new code, not recovered originals — please run a real
build and treat these two files as needing review, especially
`PackageQueryManager`'s Shizuku permission/mode-selector semantics, which I
inferred from call sites rather than an original implementation.

AGP 9.4 itself is still in preview (alpha) upstream — that's a real,
independent risk regardless of anything in this patch.

## Round 3 — real CI errors from your first AGP-9.4 build attempt

The build got much further this time and failed on real, specific errors
instead of the earlier structural ones. Root cause behind almost all of
them: **`otherLegacy` compiles against a much older Compose BOM
(`2025.01.00`) than `play`/`other` (`2026.02.00`)** — see `app/build.gradle.kts`.
Shared code in `src/main` has to compile against *both*, and several things
I wrote only existed in the newer one.

- **`ComposeCompat.kt` `layout {}` helper** — I'd written
  `androidx.compose.ui.layout.layout { ... }` as if it were a free function;
  it's actually a `Modifier` extension and can't be called that way. Rewrote.
- **`animateItemCompat` — wrong receiver entirely.** I'd assumed the
  `DashboardSettingsBottomSheet.kt` drag-reorder list was a manual
  `Box`-based implementation with no `LazyItemScope` available, so I wrote a
  custom `Modifier` extension with my own placement-animation logic. Wrong —
  it's actually called inside a `LazyColumn`'s `itemsIndexed {}` block, so
  `LazyItemScope` *is* available. Rewrote as a `LazyItemScope` extension
  that just delegates to the real, built-in `animateItem()`. Much simpler,
  and removes all the custom animation code that was solving a problem that
  didn't exist.
- **`rememberOverscrollEffect()` is `internal`** in this project's Compose
  Foundation version — app code can't call it. Now always returns `null`
  (every call site already treats that as "use default overscroll").
- **`Modifier.verticalScroll(state, overscrollEffect)`** — that 2-arg
  overload doesn't exist on the older BOM. Now ignores `overscrollEffect`
  entirely and calls the 1-arg form.
- **`WindowSizeClass.isWidthAtLeastBreakpoint()` / `WIDTH_DP_MEDIUM_LOWER_BOUND`**
  — newer-BOM-only breakpoint API. Replaced with a plain
  `LocalConfiguration.current.screenWidthDp >= 600` check, which is
  version-agnostic and has been stable Compose API for years. This changed
  `isWidthAtLeastBreakpointCompat` from a `WindowSizeClass` extension to a
  plain `@Composable` function, so the 3 call sites (`MainActivity.kt`,
  `QRSDialog.kt`, `LogScreen.kt`) had the `windowSizeClass.` receiver prefix
  dropped.
- **`ExposedDropdownMenuAnchorType`** — same BOM-skew pattern, newer-BOM-only
  enum. Added `menuAnchorCompat()` using the older, deprecated-but-still-
  supported no-arg `menuAnchor()` overload (Google's own docs note it's
  "maintained for binary compatibility").
- **`PackageQueryManager.kt` — `rikka`/`Shizuku` unresolved on `otherLegacy`.**
  This one wasn't a mistake in the code itself so much as a wrong dependency
  assumption: `dev.rikka.shizuku:api` is deliberately scoped to `play`/`other`
  only (API 23+, see the comment above it in `app/build.gradle.kts`) —
  `otherLegacy` (API 21+) was never meant to have it. My `PackageQueryManager.kt`
  lives in shared `src/main` and imported `rikka.shizuku.*` directly, which
  can never work for `otherLegacy`. Fixed properly rather than papered over:
  added `vendor/ShizukuBridge.kt` (a dependency-free interface, in `src/main`)
  with two implementations — a real one in `src/minApi23/java` (merged into
  `play`+`other`, already existing infrastructure in this project's own
  `sourceSets` block, just previously unused) and a no-op stub in
  `src/minApi21/java` (merged into `otherLegacy`). `PackageQueryManager.kt`
  now only talks to the interface, never `rikka.shizuku.*` directly.

### ⚠️ One thing I can't verify from here — please check

Your `git status` before this push showed an **untracked
`app/src/main/java/io/surprise/ciphertun/compat/WindowSizeClassCompat.kt`**
sitting in your local tree already, separate from the `ComposeCompat.kt` I
provide. My `ComposeCompat.kt` also declares `object WindowSizeClassCompat`
in the same package (`io.surprise.ciphertun.compat`). If that other file
*also* declares an object with that name, Kotlin will fail with a
redeclaration error the moment both are present. I have no visibility into
that file's contents — please open it and check, or delete it if it's stale
scratch work from an earlier session, before your next build.

### Still unresolved from before this run
- The `git push` was rejected (`fetch first` — remote `dev` has commits your
  local clone didn't have) right before you pasted this CI log. That was
  never resolved as far as I can tell — you'll need to reconcile before
  pushing this round's fixes. See the reply for the exact commands.
