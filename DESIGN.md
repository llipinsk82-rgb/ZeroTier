# DESIGN.md — BlackServ ZeroTier TV

**Platform:** Android TV / Google TV, 10-foot interface, D-pad only as baseline.
**Subject:** a physical network switch between verified Polish gateway tuners; no fictitious hosts.
**Audience:** Lucas and family on TCL in the UK. Single job: see the real exit connection and change it reliably.

## Visual direction
Practical, calm network-control console rather than a phone VPN skin. The signature is a horizontal network-link indicator joining the selected gateway tile to the current public exit IP. **Never light it green based solely on pressing OK.**

## Tokens
- `canvas`: `#10191E` — graphite, full screen
- `surface`: `#1D2D31` — resting gateway tile
- `focus`: `#95E1D9` — conspicuous 4 dp D-pad outline, not the connection state
- `connected`: `#3AA37A` — verified VPN *and* egress
- `disconnected`: `#829397` — no tunnel
- `pending`: `#F1BB60` — connecting/reconfiguring/unverified
- `error`: `#D85B65` — network or gateway failure
- `text`: `#F4F8F7`, `muted`: `#B4C5C5`

## Typography
- Display/headers: system sans with medium/bold weight, 26–32sp.
- Card labels: system sans 22–26sp, remote-readable; ID and IP in system monospace 16–18sp.
- Auxiliary data: 16–18sp minimum, unobscured by overscan.

## Layout and behavior
- 48dp screen-safe insets; horizontal or two-column grid depending on number of hosts.
- Tile minimum height 112dp; no control reachable only by touch.
- Every action has a visible focus, pressed/pending/confirmed/error state, text label as well as color.
- No uncontrolled animation; 150–200ms state feedback maximum, respect system reduced motion.
- Selecting new gateway requires route exclusivity, explicit feedback, and rollback to known good when supported.
- True gateway identity comes from BlackServ Panel/API and manually verified ZeroTier mappings, never from a numeric IP guess.
- Home / Back behavior follows Android TV expectations. System VPN consent remains native and may need remote navigation.

## Language
Polish primary, concise labels: „Połącz”, „Rozłącz”, „Łączenie”, „Połączono”, „Błąd”, „Aplikacje poza VPN”. Network/tuner aliases are data; do not translate.

## Runtime ownership
Colors/spacing will be placed in shared Android resources (`values/colors.xml`, `dimens.xml`), not copied independently across layouts. This document is the normative reference until imported upstream resources are inspected.

## Full-client revision 2026-11-04
- A single TV launcher and one Settings dialog own navigation. No phone network-list entry on home.
- Two predefined editable profiles use controller-advertised next hops verified read-only on 2026-11-04. They are never automatically connected.
- All applications go through VPN by default. Exceptions are selected explicitly; toggling a row preserves D-pad focus.
- JSON import validates the full document before a confirmation replaces settings. It disconnects, preserves identity, and does not reconnect.
- Diagnostic services: ipify (IPv4), ipwho.is (approximate IP geography), Cloudflare (bounded HTTPS throughput), bash.ws (authoritative DNS observations). Service errors never manufacture PASS.
- Explicit IPv4 exits block IPv6 until a real IPv6 gateway is verified. DNS settings accept IPv4/IPv6 literals, but IPv4 exit mode requires at least one IPv4 DNS.
