# Android TV ZeroTier MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real Android TV ZeroTier client controlled with the D-pad, with safe VPN switching and per-app bypass.

**Architecture:** Fork the exact ZerotierFix source and its pinned ZeroTierOne native core. Preserve the existing VPN service, add a D-pad-first TV Activity and a small package exclusion configuration; introduce gateway routing only after confirmed ZeroTier network topology/Panel mappings.

**Tech Stack:** Java 11 sources, Android Gradle Plugin 7.4.2, Gradle 7.5, JDK 17, Android SDK 33, NDK/CMake required by upstream, JUnit 4.

**Spec:** `docs/design/2026-11-04-android-tv-mvp.md`

## Global Constraints
- No TV operations/install until agreed test window; existing official ZeroTier continues.
- One active VPN and gateway at a time; never show a green egress badge without IP verification.
- Do not infer BlackServ tuner aliases, ZT network IDs or gateway IPs.
- Preserve GPL source/notices and native core submodule reference; avoid trademark-confusing icon.
- Android TV D-pad, focus indicator, Leanback launcher, Polish strings.
- No GitHub Actions; local build/test only.

## Review Focus
1. Empty/saved null bypass preferences: null gets defaults; explicit empty means no exclusions (Task 2).
2. Duplicate/malformed package names: filtered and deduplicated (Task 2).
3. Full-tunnel network with `routeViaZeroTier=true`: bypass must still apply (Task 2).
4. Connecting a different network while VPN is already active: never leave two active sessions, show pending until actual confirmation (Task 3).
5. Identical ZT network with multiple gateway tuners: refuse to pretend switching VPN networks selects a tuner (Task 4).

---

### Task 1: Reproducible local build

**Files:**
- Modify: `app/build.gradle`, `app/src/main/AndroidManifest.xml` only as supported by build evidence.
- Create: `docs/build.md`.

**Interfaces:** Gradle `assembleDebug` and `testDebugUnitTest`; output APK not installed.

- [ ] Inspect upstream SHA, status, licenses, git submodules and Gradle build.
- [ ] Set up portable JDK 17 and Android SDK under `C:\BlackServDev\tools`; install only required SDK/NDK/CMake.
- [ ] Run baseline `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`; record exact failures or artifact SHA.
- [ ] Preserve working source on independent branch and document a reproducible command.
- [ ] Commit tested state; no deploy.

### Task 2: Per-app VPN bypass

**Files:**
- Create: `app/src/main/java/net/kaaass/zerotierfix/service/VpnBypassConfig.java`.
- Modify: `app/src/main/java/net/kaaass/zerotierfix/service/ZeroTierOneService.java`.
- Create: `app/src/test/java/net/kaaass/zerotierfix/service/VpnBypassConfigTest.java`.

**Interfaces:** `VpnBypassConfig.parse(String): List<String>`, `serialize(Collection<String>): String`, `PREF_KEY`.

- [x] Write 5 failing package parser unit tests.
- [x] Verify RED by compiling tests against missing parser (6 unresolved symbol errors).
- [x] Implement minimal parser and serializer.
- [x] JUnit 4.13.2 on portable JDK: 5/5 PASS.
- [ ] Add service-level test covering bypass application regardless of `routeViaZeroTier`.
- [ ] Apply exclusions to `VpnService.Builder` before `establish()`, respecting app package visibility and failures.
- [ ] Add settings screen/list with installed TV apps and explain reconnect requirement.
- [ ] Full Gradle tests, diff review, commit.

### Task 3: Android TV D-pad shell and VPN controls

**Files:**
- Create: `app/src/main/java/net/kaaass/zerotierfix/ui/tv/TvHomeActivity.java`, `app/src/main/res/layout/activity_tv_home.xml`.
- Modify: `app/src/main/AndroidManifest.xml`.
- Test: `app/src/test/java/net/kaaass/zerotierfix/ui/tv/...` for controller state.

**Interfaces:** Existing `NetworkDao`, `ZeroTierOneService.ZT1_NETWORK_ID`, ZeroTier EventBus network/node status.

- [ ] Test idle/connecting/active/error state transitions, including stale events.
- [ ] Build a focusable tile for each actual configured network, with D-pad OK and descriptive status.
- [ ] Add explicit safe STOP/START sequencing and VPN consent flow; never equate a network with a tuner.
- [ ] Add Leanback launcher intent, app banner/icon and Polish labels.
- [ ] Verify hardware focus and Back flow in Android TV emulator or actual test window.

### Task 4: Verified gateway mapping and release

**Files:** New explicit `Gateway` config/model + tests; service route override only after inspection.

- [ ] Read-only fetch actual tuner IDs/names/models/online state and ZeroTier IP mappings from BlackServ Panel.
- [ ] Determine whether gateway selection requires routed next-hop override within one ZeroTier network.
- [ ] Test selection logic for one and multiple gateway mappings; no guessed routes.
- [ ] Verify actual egress IP/country and split app behavior end-to-end; green status only after this test.
- [ ] Package/sign debug/release, define rollback and test on TV with owner when free, then commit/push/health.

## Current blockers
- Native SDK/NDK toolchain still being installed; no full APK build evidence.
- Local GitHub CLI running as SentinelX SYSTEM does not inherit the user's interactive `gh auth`; GitHub connector writes work.
- Repository initially contained only README; complete upstream source import/push is pending.
