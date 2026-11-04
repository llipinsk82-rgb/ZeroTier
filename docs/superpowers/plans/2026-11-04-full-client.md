# Full Android TV client implementation plan

Goal: a real VpnService/JNI client with TV controls, no automatic app exclusions, local JSON backup and honest diagnostics.
Architecture: retain pinned ZerotierFix and its native ZeroTierOne submodule; publish additive patch 0005, reproducible bootstrap, no Actions. Existing separate package stays unchanged.
Spec: Lucas's five-stage request, superseding previous MVP defaults.

1. Audit all three refs and native build; verify TCL ABI and current tuner identities through existing operator CLI. Never infer gateway from public IP.
2. Regression: fresh bypass empty (RED then GREEN). Fix service foreground startup, stop ordering, authorization/error reporting and timeout recovery; preserve system consent.
3. Unify TV navigation: two editable profiles, one settings menu, network creation with profile save, stable D-pad focus and single launcher.
4. JSON export/import through Storage Access Framework; validate completely before committing, don't transfer identity or reconnect on import. Diagnostics perform real HTTPS probes and label unavailable DNS evidence as inconclusive.
5. Reproduce four baseline patches plus new patch on a clean Windows checkout; run unit tests, assemble/lint, verify native armeabi-v7a and signature. Publish separate branch and draft PR with remaining device/exit tests explicit.

Review focus: denied network; stale async probe after switch; absent/invalid backup; missing document picker; service crash/disconnect leaving a TUN; dynamic public IP; foreground/background differences.

Status: audit found existing JNI integration, historical 47-test build only; fresh bypass regression reproduced on BS-TEST01. Windows build host and TCL Android 14 armeabi-v7a verified read-only. No TV install or VPN change yet.

Checkpoint: 1.1.1 deletion fix built locally; 60 tests PASS, lint 0 errors/155 warnings. Clean six-patch reconstruction matches the implementation. Device VPN acceptance remains open; IPv6 is outside the private-app scope.
