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

## Round 4 — down to 2 errors, both in menuAnchorCompat

- **Missing `@OptIn(ExperimentalMaterial3Api::class)`** — `ExposedDropdownMenuBoxScope`/
  `menuAnchor()` are marked experimental in Material3; using them without
  opting in is a hard compile error, not just a warning. Added.
- **Wrong receiver shape.** The real `menuAnchor()` is a *member-extension*:
  simultaneously a member of `ExposedDropdownMenuBoxScope` and an extension
  on `Modifier` — it needs both receivers at once. My version was declared
  as a plain `ExposedDropdownMenuBoxScope` extension, but the call site
  chained it directly onto a `Modifier` (`Modifier.fillMaxWidth().menuAnchorCompat()`),
  so the scope receiver was missing at the call site → "receiver type
  mismatch." Fixed by taking `modifier` as an explicit parameter instead of
  chaining, which lets both receivers resolve correctly:
  `menuAnchorCompat(Modifier.fillMaxWidth())`. Only one call site
  (`ProtocolFormScreen.kt`), updated to match.

Confirmed via full-repo grep this is the only `ExposedDropdownMenuBox`/
`menuAnchor` usage anywhere in the codebase — nothing else needed touching.
