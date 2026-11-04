# BlackServ ZeroTier TV

Android TV / Google TV client based on the GPL-2.0 open-source
[ZerotierFix](https://github.com/kaaass/ZerotierFix) project.

**Status: development — not an installable release.**
On 4 November 2026: 46/46 standalone Java tests pass, 89/89 Java source
files parse, modified XML parses. Android SDK install/build is blocked
until the owner accepts the Google SDK license. No APK and no TV testing yet.

## Source layout

- `app/src/...`: tested standalone TV model sources (development snapshots).
- `patches/0001-blackserv-zerotier-tv.patch`,
  `patches/0002-disable-ipv6-for-explicit-ipv4-exit.patch`, and
  `patches/0003-verify-pinned-public-ipv4-exit.patch`: complete ordered
  patches against pinned upstream
  `0373a58c03ffe053e92212a6fa2d98245db8aa9c`, including Android TV
  activities, routing changes, resources and JUnit tests.
- `scripts/bootstrap.ps1`: checks out the upstream Android project and applies
  all patches in a **new** directory without overwriting existing files.
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
request a per-tuner IPv4 exit route and disables IPv6 for this mode to
avoid falling through to the underlying UK connection. An HTTPS check against
`api.ipify.org` compares the TV's observed public IPv4 against the pinned,
user-verified Polish public IPv4 for each tuner. Only an exact match turns
its tile green. Without a pinned expected IP, the tile remains yellow; a
mismatch or repeated timeouts surface an error. HTTP verification is
implemented but **not yet built, installed, or tested on Android TV**.
DNS checks, safe automatic switching and production smoke tests are pending.
The TCL television has not been touched.

Preserve upstream GPL-2.0 license and copyright notices when distributing
source or APK.
