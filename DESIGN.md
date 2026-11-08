# BlackServ ZeroTier TV — Design Context

## Product
Remote-first VPN client for a TCL Google TV. A household user switches between verified Polish ZeroTier exit tuners using only a D-pad remote.

## Visual language
- Canvas: deep petrol #0B141B, not pure black.
- Surfaces: #14232C. Bright focus: sea-glass #8CE4D5.
- Verified: #3AA37A; connecting: #F1BB60; failure: #D85B65.
- Native two-column 16:9 dashboard: active session and network facts on the left;
  compact list of at least three tuner rows on the right. No WebView or HTML.
- Readable system sans-serif labels; focus ring must remain visible without touch.
- The selected tuner and *verified* internet exit are separate states.

## Navigation
- D-pad Up/Down between focusable tuner cards and actions; OK activates; Back returns.
- Visible quick actions: DNS, Export, Add Tuner, Settings.
- Editing, deleting and duplicating profiles are distinct operations.
- Never mark a tunnel green solely because the ZeroTier network joined.
- Avoid spontaneous modal dialogs during playback or reconnect.

## Resilience
- A missing internet connection is not evidence that a tuner has failed.
- Recover the active tunnel through the existing VpnService; never modify JNI or create a second ZeroTier node.
- Changes to VPN authorization must remain user initiated.
- Each tuner has its own DNS. Default comes from its ZeroTier controller;
  manual Cloudflare, Google, Orange PL or custom address is opt-in.
- If the selected controller supplies no DNS, establishment fails closed.
- Automatic failover only after bounded retries, hysteresis and cooldown.

## Source
- Android shell derived from kaaass/ZerotierFix 0373a58.
- ZeroTier JNI submodule pinned to d028a247.
- BlackServ TV artwork reused from the earlier project.
