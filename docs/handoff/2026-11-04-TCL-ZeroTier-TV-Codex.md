# BLACKSERV ZERO TIER TV — HANDOFF DLA CODEX / CTO

**Data:** 2026-11-04 (UK). **Status:** częściowa poprawka domyślnego VPN bypass wykonana lokalnie, pełna aplikacja NIEGOTOWA. Nie instalować ani nie oznaczać jako PASS bez testów na TCL.

## 1. Zlecenie użytkownika

Lucas chce **pełnoprawnego klienta ZeroTier dla pilota TCL Google TV**, a nie graficznej nakładki na drugi, telefoniczny interfejs. Ekran główny ma zawierać rzeczywiste tunele/bramy **ID5 i ID9**, ich status (offline, odmowa/autoryzacja, łączenie, zweryfikowany polski egress), jeden przycisk Połącz/Rozłącz i przełączenie między bramami bez utraty zwykłego Internetu. Aplikacje wymagające polskiego IP (w szczególności **PlanetaTV**) mają iść przez polski tunel. Wymagane: D-pad, brak fałszywego zielonego statusu, przyjazne błędy i brak fikcyjnych adresów.

## 2. Git, upstream, build

- Repo: **`https://github.com/llipinsk82-rgb/ZeroTier`**.
- **Kod jest na `cto/android-tv-mvp-20261104`, nie na `main`** (tam niemal wyłącznie README). HEAD źródła przy audycie: `2b072f9ccef56359604934352e24a217de0a314a`.
- Repo zawiera **tylko własne pliki/łatki**, a nie pełną kopię upstream. `scripts/bootstrap.ps1` klonuje `kaaass/ZerotierFix` @ `0373a58c03ffe053e92212a6fa2d98245db8aa9c` z submodułem ZeroTierOne, po czym po kolei nakłada `patches/0001..0004`. Bez bootstrapu nie ma projektu Android do kompilacji. Nie pomijać łatek 0002/0003 (IPv6 leak/fail-closed egress) ani 0004 (ZeroTier Java SDK).
- Skompilowany wcześniej debug APK: `uk.blackserv.zerotiertv` v1.0.10 / versionCode 14, z `net.kaaass.zerotierfix.ui.tv.TvHomeActivity` jako launcherem. Repo podaje `47` dawnych testów PASS i debug APK SHA `F99475FC1ABE78183A5242473B823DE28799E9620D79EC2DB563723FA0D383DE`; **to historyczny build sprzed obecnej poprawki**, nie dowód działania na TV.
- Maszyna build/test: **SentinelX Windows `BS-TEST01`** (host ID `host_a711939dc741ef09`), istniejący lokalny Android SDK/JDK/Gradle; instrukcja i `GRADLE_USER_HOME=C:\BlackServDev\g` w `README-BLACKSERV-TV.md`. Unikać GitHub Actions.
- Zacząć od `git status`, SHA, aktualnych diffów, a potem checkout/fresh bootstrap w osobnym folderze i testów. Nie nadpisywać nieznanych zmian.

## 3. Zdiagnozowane błędy — dowód w kodzie

### A. Dwie listy, żadnego automatycznego powiązania

- `patches/0001-blackserv-zerotier-tv.patch` tworzy `TvHomeActivity`.
- `TvHomeActivity.refreshCards()` odczytuje **tylko `TvGatewayStore.load(this)`** (osobne `SharedPreferences`). Gdy brak profili, mówi „Nie dodano jeszcze żadnego tunera”.
- Upstream `JoinNetworkActivity` zapisuje członkostwa ZeroTier do GreenDAO `NetworkDao` (`ztfixdb`), **nie do `TvGatewayStore`**. Właśnie dlatego na TCL są dwie zapisane sieci, a kafelki nadal puste.
- Akcja `Sieci ZeroTier` w `TvHomeActivity` otwiera `JoinNetworkActivity` zamiast listy `NetworkListActivity`; użytkownik widzi dodawanie nowej sieci zamiast tego, co już jest.
- `showAddDialog()` wymaga również prawdziwego adresu IPv4 bramy, którego jeszcze nie ustalono, więc nie ma możliwości sensownie dodać dwóch kafelków bez zgadywania adresów.

**Zalecana poprawka:** jedna lista w TV budowana z realnych sieci w `NetworkDao`, wzbogacona (opcjonalnie) o zweryfikowany profil bramy w `TvGatewayStore`. Rozdzielić status *członkostwa w sieci* od statusu *polskiego wyjścia*. Preset #5/#9 może pokazać zweryfikowaną nazwę i Network ID z komunikatem „brak autoryzacji / brak danych bramy”; nie może włączyć domyślnej trasy ani zaświecić się na zielono bez pełnej konfiguracji. „Dodaj sieć” musi być jedną spójną operacją z aktualizacją obu widoków, bez dublowania formularzy. Nie usuwać `TvGatewayStore` ślepo: `gatewayIpv4` i `expectedPublicIpv4` to osobne, niezbędne dane bramy, których `NetworkDao` nie przechowuje.

### B. PlanetaTV była **domyślnie pomijana przez VPN**

- `app/src/main/java/.../service/VpnBypassConfig.java`, lustrzana implementacja w `patches/0001-*.patch`, miały `DEFAULT_PACKAGES = "com.android.vending,com.planetapl.tv.Player"`.
- To stoi w sprzeczności z wymaganiem użytkownika: PlanetaTV ma dostawać **polski IP**.
- Rozpoczęta poprawka: default pozostawia poza VPN tylko `com.android.vending`; `app/src/test/.../VpnBypassConfigTest.java` dostosowany. Uaktualniona również specyfikacja w `docs/design/2026-11-04-android-tv-mvp.md`. Lokalny test Java wykonał **RED** na starej wartości (AssertionError), a po zmianie **GREEN**. To NIE zastępuje pełnego Android Gradle testu.
- Potwierdzenie łatki: wszystkie cztery patche **0001 → 0004** przeszły `git apply --check` i zastosowały się bez błędu na czystym `kaaass/ZerotierFix` @ `0373a58...`. Nie wykonano pełnej kompilacji Android APK w tej sesji.
- **Uwaga migracyjna:** jeśli na TCL użytkownik jawnie zapisał listę wyjątków zawierającą PlanetaTV, zmiana defaultu (null) jej nie usunie. Sprawdzić rzeczywiste zapisane preference i zapewnić opcję jawnego odznaczenia; nie kasować świadomego wyboru bez zgody.

### C. Niepoprawne wrażenie „połączono”

- Core `ZeroTierOneService` i VPN po zgodzie Androida realnie uruchamiają węzeł, ale to nie potwierdza autoryzacji sieci ani polskiego egress.
- `patches/0003` dodaje `VirtualNetworkStatus.NETWORK_STATUS_OK` i `TvExitProbe` (`api.ipify.org`) porównywany z **ręcznie potwierdzonym** publicznym IP. Zachować fail-closed statusy; dopisać testy stanów `Access Denied`, brak IP, błędny IP, rozłączenie i przełączenie.
- **Nie utożsamiać Network ID i tunera**; dla konkretnej bramy trzeba ustawić poprawny ZeroTier IPv4 next-hop w `ZeroTierOneService`, nie sam network join.

## 4. Fakty z testu TCL / ADB

- TCL Google TV Android 14, `192.168.1.71:33369` w lokalnym Wi-Fi. ADB działało z `BS-TEST01`; adres/port **mogą się zmienić**.
- Package: `uk.blackserv.zerotiertv`. Dwie sieci zostały dodane przez upstream UI i znalezione w `ztfixdb`:
  - **ID5 `u89.romek`** → ZeroTier `05b1a061a1000001`; znana sieć LAN `192.168.18.0/24`.
  - **ID9 `u37.romek`** → ZeroTier `c0edc64972000001`; znana sieć LAN `192.168.100.0/24`.
- Nowy **ZeroTier Node ID TCL: `f530b812b4`**. Stan węzła po starcie: `ONLINE`, lecz **OBIE sieci: `Access Denied`** (prywatne kontrolery nie autoryzowały tego node ID). Brak przydzielonych IP / przetestowanej bramy; nie ma polskiego IP.
- Po testach oba przełączniki sieci ustawiono OFF, proces tunelu nieaktywny, zwykły Internet TCL działał (ping `1.1.1.1` PASS), Spotify przywrócone na pierwszy plan. Nie zgłaszać tunelu jako połączonego.
- Dla bramy #5 i #9 **NIE MA jeszcze potwierdzonego aktualnego ZeroTier gateway IPv4 ani aktualnego polskiego publicznego IP**. Historyczne adresy telefonów/Android boxów i lokalne `eth0` tunera nie są dowodem adresu bramy.
- Realny test VPN/egress tylko gdy kontroler autoryzuje node `f530b812b4`, trasa/NAT/DNS są prawidłowe i jest wolne okno na TCL (Gosia używa telewizora).

## 5. Kontrolery i bezpieczeństwo

- ZeroTier Central nie jest źródłem prawdy dla tych prywatnych kontrolerów — są **na tunerach**.
- `Panel/api/zerotier_dispatch.py` mapuje ID5/ID9; `POST /api/v1/zerotier/<id>/pair` / Easy Pair **wyłączony produkcyjnie** (`BLACKSERV_ZEROTIER_PAIR_ASYNC_READY`), zgodnie z dawnym handoffem; nie włączać flagi na skróty.
- Panel REST bez aktywnej sesji zwrócił 401 `login required`. Nie obchodzić uprawnień przez bazę, SSH ani niezweryfikowany helper. Uzyskać autoryzowany kanał do zarządzania tymi konkretnymi tunerami, najpierw read-only: tożsamość, model, heartbeat, `listnetworks`, kontroler, `listpeers`, przypisany IP, NAT/masquerade, DNS. Dopiero potem autoryzować node według istniejącej procedury. Nie restartować tunerów / usług produkcyjnych bez potrzeby.
- Polska brama musi routować internet jako exit i działać po restarcie. Wyłączenie tunelu musi przywrócić normalny Internet UK. Domyślny IPv6 nie może przeciekać przy polskim IPv4-only exit.
- Nie wypisywać tokenów, kluczy kontrolera, haseł ani prywatnych danych użytkownika do logów/commitów.

## 6. Kolejność następnych zadań i odbiór

1. **Reprodukuj build** na podstawie brancha CTO + czterech łatek; potwierdź bazę i status repo. Uruchom pełne `:app:testDebugUnitTest :app:assembleDebug` i `git apply --check` wszystkich łatek. Nie modyfikuj działającego APK przed wynikiem.
2. **Napraw UX oraz model**: lista z `NetworkDao`, status `Access Denied`, mapowanie aliasów #5/#9 do realnych network ID; formularz danych bramy osobno i tylko gdy istnieją potwierdzone IP. D-pad, fokus, Back i stan VPN po restarcie; testy jednostkowe stanów i integracji DAO/profili.
3. **Napraw przepływ**: brak dwóch niespójnych list; Połącz/Rozłącz ma obsługiwać core VPN, wyłącznie jedno exit, przełączenie/rollback, rzeczywista weryfikacja IP i DNS. Domyślnie PlanetaTV ma przechodzić przez VPN (plus kontrola zapisanych ustawień bypass na TCL).
4. **Autoryzuj TCL na obu kontrolerach** wyłącznie legalną drogą, zweryfikuj bramę #5 i #9 i polskie IP. `Access Denied` nie jest błędem ekranu TV i nie naprawi go sama kompilacja APK.
5. **Test końcowy na TCL:** oba kafelki widoczne bez ręcznego dublowania, autoryzacja i status, przełączenie #5↔#9, ipify zgodny z oczekiwanym PL IP, streaming PlanetaTV przez PL, aplikacje oznaczone jako bypass przez UK, brak IPv6/DNS leak, rozłączenie przywraca UK, restart zachowuje ustawienia. Zweryfikuj podpis i aktualizację istniejącego APK bez utraty jego danych; jeśli podpis nie pasuje, STOP i omów migrację.

**Akceptacja:** działające oba tunele + prawdziwy polski publiczny IPv4 dla wybranych aplikacji + zachowana możliwość rozłączenia. **Testy jednostkowe i zielony wskaźnik bez testu egress NIE są zakończeniem projektu.**

## 7. Przydatne pliki / polecenia

- `README-BLACKSERV-TV.md`: lista wymagań SDK / build na `BS-TEST01`, podpis, stary APK SHA.
- `scripts/bootstrap.ps1`: reproducible import upstream i łatek.
- `patches/0001-blackserv-zerotier-tv.patch`: GUI, `TvGatewayStore`, bypass, core routing.
- `patches/0002-disable-ipv6-for-explicit-ipv4-exit.patch`: IPv6 leak prevention.
- `patches/0003-verify-pinned-public-ipv4-exit.patch`: sprawdzenie publicznego IP.
- `patches/0004-include-zerotier-java-sdk-sources.patch`: SDK Java binding.
- `docs/design/2026-11-04-android-tv-mvp.md` i `docs/superpowers/plans/2026-11-04-android-tv-zerotier.md`.
- ADB: `C:\ProgramData\Android\platform-tools\adb.exe -s 192.168.1.71:33369 shell ...` przez zatwierdzony `SentinelX/BS-TEST01`.

**Nie mieszać z projektem `BlackServ-VPN-Android` (WireGuard) ani z produkcyjnym Panelem/Franek/VM102.**
