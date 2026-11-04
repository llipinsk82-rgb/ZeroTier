# BlackServ ZeroTier TV

Android TV / Google TV client based on the GPL-2.0 open-source
[ZerotierFix](https://github.com/kaaass/ZerotierFix) project.

**Status: development — not an installable release.**
On 4 November 2026: 32/32 pure Java tests pass, 85/85 Java source
files parse, modified XML parses. Android SDK install/build is blocked
until the owner accepts the Google SDK license. No APK and no TV testing yet.

## Source layout

- `app/src/...`: tested standalone TV model sources (development snapshots).
- `patches/0001-blackserv-zerotier-tv.patch`: complete patch against pinned
  upstream `0373a58c03ffe053e92212a6fa2d98245db8aa9c`, including the
  Android TV activities, routing changes, resources and JUnit tests.
- `scripts/bootstrap.ps1`: checks out the upstream Android project and applies
  the full patch in a **new** directory without overwriting existing files.
- `docs/design/`, `docs/superpowers/plans/`, `DESIGN.md`: design and plan.

### Prepare the full Android source on Windows

From a clone of this repository:

```powershell
.\scripts\bootstrap.ps1 -Destination 'C:\BlackServDev\ZeroTierTV-src'
```

The Android build then requires JDK 17, Android platform 33, Build Tools 33.0.2,
CMake 3.22.1 and NDK 25.2.9519653. Do not use GitHub Actions.

## Functional limits

Tuner names must be mapped to actual ZeroTier IP addresses. The app can
request a per-tuner IPv4 exit route; status remains yellow until real
egress IP verification is implemented and validated. IPv6 leak and DNS
checks, safe automatic switching and production smoke tests are pending.
The TCL television has not been touched.

Preserve upstream GPL-2.0 license and copyright notices when distributing
source or APK.
