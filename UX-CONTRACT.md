# BlackServ ZeroTier TV — UX contract (native premium TV beta)

## Product and sources

- Audience: household using a TCL Google TV in the UK, controlled primarily with a D-pad remote.
- Job: choose a verified Polish ZeroTier exit tuner, confirm DNS/egress, and recover from short outages without exposing a false green status.
- Language: Polish UI; technical identifiers and IP addresses retain their exact notation.
- Sources: user-confirmed tuner configuration; `DESIGN.md`; `TvGatewayStore`, `TvNetworkStore`, `TvConnectionState`, `TvExitVerification` and their tests. User consent to Android VPN is always required.
- Accessibility baseline: visible remote focus, readable labels/status, no meaning encoded only in colour; real TCL tests remain a release gate.

## Design and canonical owners

- Visual owner: `DESIGN.md` mirrors Android tokens in `app/src/main/res/values/blackserv_tv.xml`. Runtime XML is canonical.
- Premium presentation: `TvPremiumDashboard`; native two-column 16:9 remote-first surface.
- Profile list and actions: `TvHomeActivity` and `TvProfileEditor`; IDs are unique, edit replaces the existing profile, duplicate is explicit.
- Profile persistence and migration: `TvGatewayStore` / `TvGatewayCodec`.
- Active network and DNS: `TvNetworkStore` and `TvDnsPolicy`, scoped by tuner ID. Unknown DNS fails closed.
- VPN lifecycle: existing `ZeroTierOneService`; no duplicate native node or rewritten JNI.
- System-backed overlays: Android `AlertDialog`. Export writes directly to Android Downloads via MediaStore;
  import still uses `ACTION_OPEN_DOCUMENT` and may need a document-provider app on the TV.
- Feedback: status text/card labels for durable states, toast for reversible saves, dialog for failed/dangerous actions.

## Interaction ledger

| Action | Trigger | Success | Error/recovery |
|---|---|---|---|
| Connect | OK on a tuner card | Pending state until native join and public exit verification | VPN permission is requested when needed; invalid DNS blocks connection |
| Disconnect | OK on the active tuner card | No active tuner selected; foreground VPN stopped | User may reconnect by selecting the card |
| Switch | OK on another tuner card | Old ZeroTier node destroyed before creating the next one | Keep pending/error state; do not overlap native nodes |
| Edit/duplicate | Profile actions | Replace in place or insert a separately named copy | Reject duplicate IDs; retain existing data on validation error |
| Delete | Long-OK on card or profile actions | Confirmed card removal and DNS override cleanup | Cancel does nothing; network membership is not uninstalled |
| Import JSON | Settings → import | Explicit confirmation, then replace settings and remove obsolete DNS keys | Reject invalid files before mutation |
| Export JSON | Quick action or Settings → export | MediaStore Downloads receives non-secret settings without a file picker | Show storage error; never export identity or tokens |
| Diagnose | Settings → diagnostics | Report observed public IP, DNS resolvers and routes | Timeouts show uncertainty, never a fabricated PASS |

## Connection state and resilience

- Status vocabulary: `WYŁĄCZONY`, `ŁĄCZENIE`, `WERYFIKACJA WYJŚCIA`, `POŁĄCZONO`, `BŁĄD`.
- Successful ZeroTier joining **does not** mean the public Polish exit is verified. Green requires the configured expected public IP.
- No selected profile, no underlying network or no user-approved VPN: no automatic start.
- Every tuner resolves through its own configured DNS. No silent UK/Cloudflare fallback. DNS leaks in apps with their own DoH need separate device tests.
- Automatic retry is bounded and throttled: first retry the same tuner (maximum two restarts),
  then select an eligible alternative under cooldown/hysteresis. Manual choice resets recovery.
- Boot and network-return behaviour depends on Android background-service restrictions; do not label it guaranteed until tested on TCL.
- Manual profile choice always wins over automatic fallback. Do not interrupt a user with spontaneous modal dialogs during video playback.

## Device verification gate

- Automated tests: unit and Robolectric tests plus `:app:assembleDebug` on the approved GitHub branch.
- Manual TCL matrix pending: remote focus/Back, boot offline/online, Wi-Fi loss/return, three tuners, DNS leak with/without private DNS, IPv6, switch, delete/import/export, and app update/signature.
- The installed TCL app and the current build have **different signing certificates**
  (verified by apksigner on 2026-11-08). Do not overwrite/uninstall the live app
  merely to deploy the beta: build `assembleCandidate` with the `.candidate`
  package suffix for side-by-side smoke tests until the original signing key
  or a verified identity-preserving migration path is available.
