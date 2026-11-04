# BlackServ ZeroTier TV

Android TV / Google TV VPN client based on the GPL-2.0 open-source
[ZerotierFix](https://github.com/kaaass/ZerotierFix) project.

**Status (4 November 2026): debug APK built successfully; not yet tested on a TV.**

Windows build on `BS-TEST01`:

- Gradle `:app:testDebugUnitTest :app:assembleDebug`: **PASS**.
- JUnit: **47 tests, 0 failures, 0 errors, 0 skipped** (11 suites).
- Android package: `uk.blackserv.zerotiertv`; target SDK 33.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (12,898,318 bytes).
- SHA-256: `F99475FC1ABE78183A5242473B823DE28799E9620D79EC2DB563723FA0D383DE`.
- Debug signature verified (APK Signature Scheme v1/v2). This is **not a
  production-signed release**.
- Native ZeroTier JNI packaged for arm64-v8a, armeabi-v7a, x86, x86_64.
- Android TV Leanback launcher entry and VPN service declared in the manifest.

No APK has been installed on the TCL television, and no live ZeroTier exit
routing or egress-IP verification has been tested on-device.

## Source layout

- `app/src/...`: tested standalone TV model sources (development snapshots).
- `patches/0001-*.patch` through `patches/0004-*.patch`: ordered source
  patches against pinned upstream
  `0373a58c03ffe053e92212a6fa2d98245db8aa9c`. Patch 0004
  adds the upstream ZeroTier Java SDK sources to the Android core module.
- `scripts/bootstrap.ps1`: checks out the upstream Android project and applies
  all four patches in a **new** directory without overwriting existing files.
- `docs/design/`, `docs/superpowers/plans/`, `DESIGN.md`: design and plan.

## Reproduce the Android build on Windows

From this repository:

```powershell
.\scripts\bootstrap.ps1 -Destination 'C:\BlackServDev\ZeroTierTV-src'
```

Install JDK 17, Android SDK platform 33, Build Tools 33.0.2, CMake 3.22.1
and NDK (the tested SDK had versions 23.1.7779620 and 25.2.9519653).
Accept the Android SDK licenses using the official SDK manager.

In the bootstrapped Android source directory:

```powershell
$env:JAVA_HOME = '<path-to-JDK-17>'
$env:ANDROID_HOME = '<path-to-Android-SDK>'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = 'C:\BlackServDev\g'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The short `GRADLE_USER_HOME` is intentional: Windows 10 on `BS-TEST01`
has long paths disabled, and Android AAPT failed when Gradle's cache was
under `C:\Windows\System32\config\systemprofile\.gradle`. The short
cache path fixed the resource-compilation failure without changing Windows
system configuration. Use another short writable location if necessary.

Do not use GitHub Actions; build with a local runner.

## Functional limits and next tests

Tuner names must be mapped to actual ZeroTier IP addresses. The app can
request a per-tuner IPv4 exit route and disables IPv6 for this mode to
avoid falling through to the underlying UK connection. An HTTPS check against
`api.ipify.org` compares the TV's observed public IPv4 against the pinned,
user-verified Polish public IPv4 for each tuner. Only an exact match turns
its tile green. Without a pinned expected IP, the tile remains yellow; a
mismatch or repeated timeouts surface an error.

The implementation has **compiled and passed JVM tests**, but runtime
VPN authorization, route switching, DNS behavior, tunnel throughput, and
actual Polish public egress IP **remain unverified on TCL/Google TV**.
These need controlled device smoke tests before production use. A stable
release-signing key and installation/update strategy also remain to be set.

Preserve upstream GPL-2.0 license and copyright notices when distributing
source or APK.
