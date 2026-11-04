# BlackServ ZeroTier TV — specyfikacja MVP (2026-11-04)

## Cel
Natywna aplikacja Android TV obsługiwana wyłącznie pilotem, korzystająca z realnego silnika ZeroTier (fork kaaass/ZerotierFix), bez ingerencji w działającego klienta na TCL.

## Potwierdzone warunki
- Docelowo TCL 55C6K-UK, Android 14 / Google TV.
- Upstream `kaaass/ZerotierFix` master `0373a58c03ffe053e92212a6fa2d98245db8aa9c` (GPL-2.0-only), submoduł `kaaass/ZeroTierOne` przypięty do `d028a24795e4348615783f8f8b95207035595f56`.
- Oryginalny klient ZeroTier działa. Do czasu podpisanego APK, testów i rollbacku nie zmieniać ustawień TV.
- Jedna aktywna usługa VPN systemu Android na raz.

## UX
1. Ekran TV: nazwa BlackServ ZeroTier TV, node ID, stan VPN, wielkie kafelki z focus ringiem (D-pad).
2. Kafelek: etykieta tunera (tylko potwierdzona), BlackServ ID/model (tylko zweryfikowane), ZeroTier network ID, obecny status. OK przełącza ON/OFF, wybranie innego gateway rozłącza poprzedni i uruchamia nowy po potwierdzeniu.
3. Statusy: szary rozłączono, bursztyn łączenie, zielony wyłącznie po faktycznym sprawdzeniu VPN i kraju/IP wyjścia, czerwony problem. Kolor + opis tekstowy.
4. Wyjątki VPN: lista zainstalowanych aplikacji TV z checkboxami. PlanetaPLTV (`com.planetapl.tv.Player`) proponowana jako wyjątek domyślny tylko jeśli pakiet na TV istnieje; każda zmiana wymaga odbudowy tunelu. Żadnego `QUERY_ALL_PACKAGES` bez potrzeby.
5. Dodawanie sieci, wyświetlanie komunikatu błędu i możliwość powrotu pilota.

## Inwarianty
- ZeroTier network ID ≠ tuner. W jednej sieci może być wiele polskich bram: przełączanie tunera wymaga jawnego wyboru IP bramy i zmiany routingu w VPN, a nie przełączenia sieci.
- Żadnych fikcyjnych etykiet `u39.romek`, ID lub polskich adresów IP; dane z Panelu tylko po uwierzytelnionym read-only API.
- W razie błędu połączenia aplikacja pokazuje FAIL i nie udaje sukcesu.
- Separate `applicationId`, własna ikona bez korzystania ze znaku towarowego ZeroTier jako marki BlackServ, zachować licencje i źródła GPL.
- Brak deploymentu na TV bez jawnego, bezpiecznego okna testowego.

## Etapy
- P1: czysty import/fork, budowalny zestaw Gradle/JDK/SDK/NDK, baseline.
- P2: shell Android TV, D-pad, stan rzeczywisty, ON/OFF pojedynczego skonfigurowanego ZeroTier network ID.
- P3: per-app bypass pełnego tunelu, wybór aplikacji, testy.
- P4: mapowanie tunery ↔ sieć ↔ IP bramy; kontrola routingu w tej samej sieci; egress PL/UK verification.
- P5: podpis, testy na urządzeniu, fallback/rollback, release OTA w osobnym zadaniu.

## Kryteria odbioru
Udany build debug APK, testy jednostkowe, test D-pad na emulatorze/TV, realne przełączenie bez utraty kontroli, poprawne IP wyjściowe i działanie PlanetaPLTV poza VPN. Sam wynik kompilacji nie wystarcza.
