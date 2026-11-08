# BlackServ TV v3 Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Execute tasks with RED→GREEN evidence.

**Goal:** Reduce CPU/logging overhead in BlackServ ZeroTier v3 and ensure reconnect does not leave ARP/NDP threads, without changing ZeroTier JNI or approved TV UX.

**Architecture:** Preserve the recovered v3 source, native engine, DNS/failover/profile behavior. Gate per-packet diagnostic output in candidate and remove formatting from hot paths. Own auxiliary lookup threads only while a TUN adapter is running and stop them on all shutdown paths.

**Tech Stack:** Java 11, Android VpnService, Gradle, JUnit/Robolectric, GitHub Actions.

**Spec:** `DESIGN.md` and `UX-CONTRACT.md`.

## Global Constraints

- Original v3 branch `blackserv/tv-v3-recovered-20261108` remains untouched.
- The installed TCL APK, signing identity, VPN permission, tuner profiles and Node ID remain untouched.
- No JNI submodule changes or additional ZeroTier node.
- Never infer runtime CPU improvement from unit tests; require device measurements after separate approval.
- GitHub CI is the verification environment; Dell SentinelX currently denies direct `gradlew.bat` execution.

## Review Focus

- Adapter created but TUN setup fails: no ARP/NDP thread remains.
- Repeated reconnect/update: one live ARP/NDP pair at most per active adapter.
- Candidate APK: `debuggable=false` and remains side-by-side installable.
- UDP packet send/receive and tunneled IPv4/IPv6: behavior unchanged after log removal.
- Manual disconnect: no unwanted background reconnect or profile mutation.

---

### Task 1: Test protections and CI
**Files:** `app/src/test/java/net/kaaass/zerotierfix/service/TunTapAdapterLifecycleTest.java`, `app/src/test/java/net/kaaass/zerotierfix/service/CandidateBuildTypeTest.java`, `.github/workflows/blackserv-tv-debug.yml`.
**Interfaces:** Tests exercise `TunTapAdapter(ZeroTierOneService,long)` and `interrupt()`; build variant `candidate`.
- [ ] Add failing regression tests for idle adapter cleanup and non-debuggable candidate.
- [ ] Commit tests alone and verify expected RED with GitHub Actions.

### Task 2: Lifecycle and logging fixes
**Files:** `TunTapAdapter.java`, `ZeroTierOneService.java`, `UdpCom.java`, `ARPTable.java`, `app/build.gradle`.
**Interfaces:** Preserve existing adapter/service public behavior; `candidate` remains `.candidate` package.
- [ ] Allocate ARP/NDP lookup workers lazily and stop them in all shutdown states.
- [ ] Stop adapter even when receive thread is not active.
- [ ] Remove packet-by-packet logging and eager string formatting.
- [ ] Build non-debuggable, side-by-side candidate with debug test signing.
- [ ] Push and verify candidate unit suite + APK build PASS in CI.

### Task 3: Handoff
**Files:** This plan and source changes.
- [ ] Review diff/status, commit/push if green.
- [ ] Record tests and remaining TCL smoke/performance gate.
- [ ] Do not deploy on TCL during this task.
