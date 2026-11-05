# BlackServ ZeroTier TV — 1.1.5 preview

Native Android TV client based on [ZerotierFix](https://github.com/kaaass/ZerotierFix), with the ZeroTier JNI core and Android `VpnService`. Package `uk.blackserv.zerotiertv` is separate from the original `com.zerotier.one`.

**Preview: build and JVM tests are not proof of working TV egress. See the [audit and test report](docs/handoff/2026-11-04-full-client-audit.md) for evidence and remaining gates.**

## Included

- Two remote-focusable gateway cards, connection/disconnection, one settings menu.
- Dedicated 320×180 Android TV launcher banner plus a separate square system icon, so launchers do not stretch one asset into both shapes.
- Profiles for #5 and #9 populated from a fresh read-only BlackServ audit; network ID, next hop and expected public IPv4 remain editable.
- Foreground VPN service, opt-in boot restoration, tunnel health checks and early TUN closure on shutdown. Connection status requires a successful expected-public-IP check, not just a button click.
- All apps use VPN by default; exclusions require manual selection under `Aplikacje bez VPN`.
- Configurable DNS; actual public IPv4/geolocation, bounded download/upload probes and resolver-observation DNS test.
- Local JSON import/export through Android's document picker: profiles, DNS, boot preference and exclusions. No passwords, identity keys or connection sessions in backups.

## Source and reproducible build

`app/src/...` contains selected standalone model snapshots, not a complete Android project. The complete source is reconstructed using **all eight ordered patches** against ZerotierFix `0373a58c03ffe053e92212a6fa2d98245db8aa9c`. The pinned native submodule is `d028a24795e4348615783f8f8b95207035595f56`.

Windows:

```powershell
.\scripts\bootstrap.ps1 -Destination 'C:\BlackServDev\ZeroTierTV-src'
```

Linux/macOS source preparation:

```sh
./scripts/bootstrap.sh /path/to/new/ZeroTierTV-src
```

Install JDK 17, Android SDK platform 33, Build Tools 33.0.2, CMake 3.22.1 and **NDK 23.1.7779620**; accept Android SDK licenses. Build locally, without GitHub Actions:

```powershell
$env:JAVA_HOME = '<path-to-JDK-17>'
$env:ANDROID_HOME = '<path-to-Android-SDK>'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = 'C:\BlackServDev\g'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
# Run inside the generated Android source:
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Short Gradle paths avoid the observed Windows resource-compilation failure with long paths disabled. Gradle 7.5 and AGP 7.4.2 are pinned by upstream. Linux builds use `./gradlew` with equivalent environment variables; this iteration was built on Windows BS-TEST01.

Output: `app/build/outputs/apk/debug/app-debug.apk`. API 23 minimum, target 33; ARM32, ARM64, x86 and x86_64 JNI. The inspected TCL runs API 34 with **armeabi-v7a**, so an ARM64-only APK is unsuitable.

## Installation and device acceptance

1. Keep original ZeroTier installed. Install the preview APK using a local installer or `adb -s <TCL> install -r app-debug.apk`. It uses a debug signature, not a production release key. Do not uninstall a previous BlackServ install automatically if signature verification fails.
2. In a testing window, open BlackServ from the TV launcher and select a profile. Android requires VPN consent and replaces the active VPN; television streams may stop during switching.
3. Authorize the **new installation's actual node ID** on each selected controller/network. Do not reuse historical device IDs or copy the original app's identity. Inspect new node information in diagnostics/logs if access is denied.
4. Confirm green status and the expected Polish IPv4. Test both gateways, D-pad navigation, Back, app exclusions, DNS results, background streaming, Wi-Fi loss/recovery, VPN revocation and reboot with opt-in enabled. Compare network routes and public IP from inside and outside an excluded app.
5. Stop BlackServ and verify internet recovery. To roll back, stop/force-stop `uk.blackserv.zerotiertv`, then reconnect the original ZeroTier manually. No tuner or router changes were made by this work.

## Known limits

- Explicit IPv4 exits **block IPv6** to prevent bypass. Full IPv6 exit requires a verified gateway IPv6 route/address/forwarding design and is not delivered by this preview.
- The public IP match is intentionally strict. If an ISP changes the address, verify and update the profile; otherwise health checks stop the tunnel. Three consecutive failed probes also stop it; transient endpoint failures can therefore disconnect a working VPN.
- Health checks and shutdown recovery need on-device fault tests. Thread joins can still delay service teardown; early TUN closure reduces route retention but is not proof against every OEM/process failure. Android's user-enabled lockdown mode can intentionally block internet after VPN loss.
- Boot restoration depends on retained VPN consent and Android/OEM background-start rules. Force-stopping the app suppresses normal boot handling until reopened.
- DNS diagnostics report observed resolvers and inconclusive results honestly; they do not certify another app's private DoH traffic. Resolver country alone is not proof of a leak. Diagnostics contact ipify, ipwho.is, Cloudflare and bash.ws and expose the normal public request metadata to those services.
- JSON export needs an installed Android document provider. Settings import excludes ZeroTier identity and requires authorization on a new device.
- TCL acceptance on 2026-11-05 passed for tuner #5: node `38c76e1aae`, ZeroTier address `192.168.18.246`, bidirectional reachability, DNS, full-tunnel public IPv4 `91.227.216.230` and Polish geolocation. The underlying `192.168.1.0/24` LAN remains outside the VPN, preserving ADB access.
- Tuner #9 is authorized with `192.168.100.248`, but live exit-IP acceptance still requires an approved TV switching window. Tuner #23 uses an external controller and cannot be authorized from the tuner itself; controller access is required.
- Throughput, reboot restoration, disconnect fault recovery and prolonged background playback remain unverified. Stable release signing/update strategy is still required.

Retain ZerotierFix's GPL-2.0 notices and the native ZeroTier core's own license, copyright and distribution terms; the native core is not covered merely by describing the Android wrapper as GPL.
