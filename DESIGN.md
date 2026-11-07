# BlackServ ZeroTier TV — Design Context

## Product
Remote-first VPN client for a TCL Google TV. A household user switches between verified Polish ZeroTier exit tuners using only a D-pad remote.

## Visual language
- Canvas: deep petrol #10191E, not pure black.
- Surfaces: #1D2D31. Bright focus: sea-glass #95E1D9.
- Verified: #3AA37A; connecting: #F1BB60; failure: #D85B65.
- Large readable system sans-serif labels. Focus state must remain visible without touch.
- The selected tuner and *verified* internet exit are separate states.

## Navigation
- D-pad Up/Down between focusable tuner cards and actions; OK activates; Back returns.
- Editing, deleting and duplicating profiles are distinct operations.
- Never mark a tunnel green solely because the ZeroTier network joined.
- Avoid spontaneous modal dialogs during playback or reconnect.

## Resilience
- A missing internet connection is not evidence that a tuner has failed.
- Recover the active tunnel through the existing VpnService; never modify JNI or create a second ZeroTier node.
- Changes to VPN authorization must remain user initiated.
- Each tuner has its own DNS; an unverified DNS configuration must not silently fall back to UK DNS.
- Automatic failover only after bounded retries, hysteresis and cooldown.

## Source
- Android shell derived from kaaass/ZerotierFix 0373a58.
- ZeroTier JNI submodule pinned to d028a247.
- BlackServ TV artwork reused from the earlier project.
