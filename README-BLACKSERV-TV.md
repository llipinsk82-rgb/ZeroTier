# BlackServ ZeroTier TV — work in progress

Android TV / Google TV interface on top of the GPL-2.0 ZerotierFix Android
client, pinned upstream to commit `0373a58c03ffe053e92212a6fa2d98245db8aa9c`.

## Features under development

- Remote/D-pad compatible tuner tiles: tap toggles a single selected gateway;
  long-press asks to remove a saved profile.
- Explicit profiles with `tuner ID`, label, 16-hex ZeroTier network ID and
  the **ZeroTier IPv4 of the tuner**. More than one tuner may share a network.
- Full-tunnel IPv4 route may use the selected tuner as gateway. The route table
  now chooses the longest matching prefix, not hash-map iteration order.
- VPN per-app exclusion list, defaulting to Google Play and PlanetaPLTV,
  editable on the television (effective after reconnect).
- Separate Leanback Android TV entry point. Original phone UI remains.

## Hard limitations / no production deployment

- **Not verified on a real TV.** Native Android SDK build is currently
  blocked pending the owner's acceptance of the Google Android SDK license.
- A ZeroTier join is not proof of Polish egress. Status is deliberately
  **VERIFYING_EXIT**, never green, until real public IP verification exists.
- No automatic live switching between gateways until safe stop-and-start
  sequencing is verified.
- No IPv6 leak / DNS / failover / performance tests yet.
- No tuner identity is fabricated: configure only verified BlackServ Panel
  hosts and their actual ZeroTier IPs.
- The GPL and original copyright notices in upstream sources must be
  preserved in redistributed APK/source.

## Development status (2026-11-04)

- Pure Java JUnit 4 tests: 34 passing.
- Java syntax parse: 86 source files, zero parse errors.
- Android Manifest and modified resource XML: structurally valid.
- Full Gradle compile, APK install, TV smoke and network egress:
  **NOT YET TESTED**.
