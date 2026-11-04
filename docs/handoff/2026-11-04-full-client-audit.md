# BlackServ ZeroTier TV — full-client audit

## Source and environment
Audited all three remote refs: main 86f6b81 (README only), cto/android-tv-mvp-20261104 2b072f9, cto/zerotier-tv-routing-audit-20261104 c7d832d. No existing PR found. New work uses cto/full-client-20261104.

Existing code is a genuine ZerotierFix fork as reproducible patches, not a self-contained Android checkout. Baseline upstream 0373a58c03ffe053e92212a6fa2d98245db8aa9c; native kaaass/ZeroTierOne d028a24795e4348615783f8f8b95207035595f56. The official ZeroTierOne java/Node SDK is a JNI binding; root is unnecessary when packet IO is handled by Android VpnService. Preserve upstream and native-core licenses; do not describe the native core as universally GPL without checking its own LICENSE.

Build: BS-TEST01, JDK 17.0.21, Gradle 7.5, AGP 7.4.2, SDK/target 33, CMake 3.22.1, installed NDK versions 23.1/25.2. Core module now explicitly pins NDK 23.1.7779620. TCL read-only getprop: Android API 34; ABI armeabi-v7a,armeabi. arm64-only APK would not work here.

## Actual gateways, read-only audit 21:51–22:03 UTC
| Profile | Panel identity | ZT network | Controller default next hop | Observed public IPv4 |
|---|---|---|---|---|
| #5 | u89.romek, h17, ARM, OpenPLi 9.2 | 05b1a061a1000001 | 192.168.18.1 | 91.227.216.230 |
| #9 | u37.romek, h17, ARM, OpenPLi 9.2 | c0edc64972000001 | 192.168.100.1 | 185.148.67.216 |

Both have fresh heartbeats, ZeroTier 1.10.2 ONLINE, IPv4 forwarding and proxy ARP enabled on eth0/overlay. Both advertise bridge=true on their private networks. No assigned overlay IPv4 on tuners; this is compatible with proxy-ARP bridging. iptables nat unavailable, filter exists; no nft executable detected. Default gateways are routers, not the tuners' own LAN addresses (.5 and .153). Public egress observed from tuner via HTTPS ipify; ipwho.is returns PL for both. These facts are point-in-time and do NOT prove the new app's egress.

Existing original com.zerotier.one is installed and active on TCL. Read-only dumpsys shows tun0 192.168.18.251/24, DNS 192.168.18.1, IPv4 default route and unreachable IPv6 default. No installation or interruption performed during audit. New client uses separate uk.blackserv.zerotiertv, and must create/authorize its own node; don't copy the original identity.

Evidence: Panel operator jobs 5261–5266. No tuner/controller/production configuration changes.

## Missing or faulty baseline
- Default exclusions included Play Store (older build also PlanetaTV); now empty.
- Phone JoinNetwork UI and separate gateway list caused disconnected configuration; profile save now ensures DAO membership.
- StartupReceiver only logged, never started VPN; foreground lifecycle missing.
- Stop path could hold TUN while joining threads; route is now closed first. Full shutdown still requires on-device stress testing.
- Egress verification only in foreground UI; background health monitor now stops a stalled tunnel after three failed probes.
- No JSON backup or diagnostics UI; added.
- IPv6 egress unverified: explicit IPv4 exit remains IPv6-blocked, not falsely called dual-stack.

## Identity transfer
JSON intentionally excludes identity.secret and controller grants. Import recreates settings/membership, keeping this installation's identity. Separate optional identity migration would require stopping the old client, secure local transfer, avoiding duplicate active node IDs, and revalidating controller authorization. It is not part of settings backup.

## Runtime gates
Build/unit success does not prove VPN, routes, split routing, reboot, recovery, focus or DNS correctness on TCL. A device test interrupts the currently active original VPN (Android supports one active VPN); require a testing window. Do not uninstall original ZeroTier. Controller authorization for the NEW node is a separate step. No GitHub Actions.

## Final local verification — 2026-11-04 22:14 UTC
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: PASS, exit 0, 87 tasks (20 executed, 67 up-to-date), final job `job_e99af75d36a9` on BS-TEST01.
- JUnit XML: **56 tests, 0 failures, 0 errors, 0 skipped**. Covers existing model/routing checks plus backup validation/defaults; does not exercise Android service lifecycle or real packet forwarding.
- Lint: **0 errors, 155 warnings**. Initial MissingTranslation error for the brand name fixed with `translatable=false`; no lint baseline/suppression blanket added. Remaining warnings include localization, accessibility, old dependencies and unused resources.
- Clean pinned checkout plus all five patches: PASS; all 16 changed source files byte-identical to working implementation. SHA-256 comparison with Windows build source also matched all 16.
- Shell bootstrap syntax and implementation/non-patch repository whitespace checks: PASS. Generated unified patches contain required space-prefixed blank context lines, which a generic repository whitespace check flags; clean patch application and source-level checks pass. No GitHub Actions used.
- APK metadata: package `uk.blackserv.zerotiertv`, versionCode 15, versionName `1.1.0-tv-preview`, minSdk 23, targetSdk 33, Leanback launcher. JNI for armeabi-v7a, arm64-v8a, x86 and x86_64.
- APK: **21,388,468 bytes**; SHA-256 `61e37eb41b8a1ec74b11f3b34fa7fa403f0a7cb893077ce31450ad38530b1761`.
- `apksigner verify --verbose`: exit 0, signatures v1 and v2 verified; debug key only. V1 metadata-entry warnings remain; v2 protects APK contents.

## Acceptance status
| Requirement | Result |
|---|---|
| Native Android APK / JNI / VpnService | Built; runtime forwarding pending |
| Two current Polish gateway profiles | Read-only identities/routes/egress verified; new-client egress pending |
| IPv4 routes + configurable DNS | Implemented; device checks pending |
| Full IPv6 exit | NOT COMPLETE: deliberately blocked for current IPv4 gateways |
| Remote UI / AT4K / background / reboot | Implemented where described; device checks pending |
| Split VPN defaults | Empty exclusions verified in JVM regression; real app routing pending |
| Diagnostics / DNS leak | Real probes implemented; device and provider response tests pending |
| JSON backup | Validation/round-trip unit tests PASS; Android document-provider integration pending |
| No permanent internet loss | Early TUN close + watchdog implemented; failure injection and lockdown/OEM behavior unverified |
| Release readiness | Draft preview, not production approved; stable signing still needed |

No APK was installed on TCL. Original ZeroTier and production gateways remain unchanged. Continue with an agreed TV test window, new-node authorization, route/egress tests for both gateways, exclusions, DNS, reboot and forced disconnect recovery. Follow installation/rollback steps in README-BLACKSERV-TV.md. A main-thread join can still stall shutdown; do not claim complete fault tolerance before stress testing.
