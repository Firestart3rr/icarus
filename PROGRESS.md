# ICARUS — Log postępu

Bieżący rejestr wykonanej pracy. Aktualizowany na bieżąco przez autora.
Pełny opis projektu: [`ICARUS.md`](./ICARUS.md).

**Legenda:** ✅ zrobione · 🔄 w toku · ⏳ zaplanowane · 💤 dług techniczny (świadomie odłożone)

---

## Faza 0 — Fundament 🔄

**Cel:** `flight-planning`, jeden endpoint: dwa punkty (start, cel) → trasa po wielkim okręgu
(great-circle) + dystans. Bezstanowo (bez bazy). Pierwszy Dockerfile.

### Środowisko i narzędzia
- ✅ Ubuntu (dual boot) na HP OMEN — środowisko developerskie
- ✅ SDKMAN! + Java 25 Temurin
- ✅ Gradle (przez wrapper, `./gradlew`)
- ✅ IntelliJ IDEA Ultimate
- ✅ Python 3 (systemowy) — do generowania danych referencyjnych w testach

### Projekt i struktura
- ✅ Skeleton przez Spring Initializr (Gradle Kotlin DSL, Java 25, Spring Boot 4.1.0, Jar, YAML)
  - Dependencies: Spring Web (webmvc), Actuator, Validation — świadome minimum, bez bazy
- ✅ Rozbiór `build.gradle.kts` linia po linii (zrozumienie zamiast kopiowania)
  - mechanizm `io.spring.dependency-management` + BOM → brak numerów wersji
  - modularyzacja SB4: startery `-webmvc`/`-actuator`/`-validation` + odpowiedniki `-test`
- ✅ `bootRun` działa; `/actuator/health` → `UP`, grupy `liveness`/`readiness` (gotowe pod K8s)

### Kontrola wersji
- ✅ Git + `.gitignore` zweryfikowany, tożsamość Git
- ✅ Klucz SSH ed25519 → GitHub; repo `icarus`, pierwszy push
- ✅ Regularne commity per zamknięty kawałek pracy

### Transformacja w monorepo
- ✅ Moduł `flight-planning/`, przeniesienie `src/` (Git rename → historia zachowana)
- ✅ `settings.gradle.kts` → `include("flight-planning")`
- ✅ **Lekcja Maven→Gradle:** blok `dependencies` parenta NIE jest dziedziczony; każdy moduł
  ma własny, izolowany classpath
- ✅ Rozdział: parent chudy (`group`, `version`, `repositories`); moduł ma Spring Boot plugin
  + startery + toolchain Java 25
- ✅ Usunięta duplikacja JUnita (startery `-test` dostarczają JUnit spójny ze Spring Boot BOM)

### Kod domenowy i obliczeniowy
- ✅ **`GeoPoint`** (pakiet `domain`) — value object jako `record`
  - uzasadnienie record: obiekt-wartość (tożsamość = zawartość, niemienny, `equals` po polach),
    a NIE „bo przenosi dane"
  - walidacja niezmienników w compact constructorze (zakresy lat/lon, `IllegalArgumentException`)
  - kolejność pól `(latitude, longitude)` — zgodna z konwencją geograficzną
  - warunek `!(w zakresie)` odrzuca też `NaN` i `Infinity` (własność porównań z NaN)
  - testy: poprawne, brzegowe (±90/±180), poza zakresem, NaN, nieskończoności
- ✅ **`GeodeticConstants`** (pakiet `domain`) — `EARTH_RADIUS_METERS = 6371008.8`
  (średni promień arytmetyczny WGS84); klasa final, konstruktor prywatny
- ✅ **`HaversineDistanceCalculator`** (pakiet `distance`) — `@Service`, bezstanowy
  - świadomie BEZ interfejsu (jedna implementacja; interfejs + Strategy dopiero przy Karneyu)
  - bezstanowość = thread-safe z definicji (ważne pod wielowątkowość Fazy 1)
  - clamp `a` do [0,1] — zabezpieczenie przed `NaN` z błędu zaokrągleń float
  - `Math.atan2(√a, √(1−a))` — stabilniejsze niż `asin`
- ✅ **Testy kalkulatora** — mocne, oparte na właściwościach:
  - niezmienniki: zero dla identycznych punktów, symetria d(A,B)=d(B,A)
  - **wartości analityczne** (dowodliwe: π·R, π/2·R) — ćwiartki i pół obwodu, tolerancja 0,1 m
  - **Golden Master** — trasy (WAW→LON, JFK→CDG) policzone niezależnie w Pythonie tym samym
    modelem i R; zgodność Java↔Python do **0,0001 m** → dowód poprawności implementacji

### W toku / następne
- 🔄 Pakiet `web` — DTO (`RouteRequest`/`RouteResponse`) + `RouteController`
  - tu wróci Bean Validation (`@Valid`, `@NotNull`, `@Range`) — na DTO, nie w domenie
  - mapowanie DTO ↔ `GeoPoint` w kontrolerze (warstwa tłumacząca)
- ⏳ Pierwszy Dockerfile

### Lekcje warte zapamiętania (z tej fazy)
- **Model domenowy ≠ DTO** — rozdzielamy, bo zmieniają się z różnych powodów (domena vs kontrakt
  API), nie tylko „bo w różnych pakietach"
- **Nazwa pakietu = rola w domenie, nie technika** (`distance`, `domain` — nie `utils`/`algorithms`)
- **Value object → record; encja → klasa** (tożsamość vs zawartość)
- **LLM: dobry do wyjaśnień pojęć, ZŁY do danych liczbowych** — asystent podał „wartości
  referencyjne" i etykietę „z PostGIS" bez uruchomienia narzędzia; prawdziwy Golden Master
  wygenerowany samodzielnie (analitycznie + Python). Rygor wpisany w prompt Profesora (pkt 11).

### Dług techniczny (świadomie odłożony)
- 💤 **Convention plugin w `buildSrc`** — wspólna konfiguracja Gradle (`group`/`version`, Spring
  Boot plugin, `repositories`). Do zrobienia w **Fazie 1**, gdy dojdzie `weather`.
- 💤 **Configuration cache** — przyspieszenie buildów. Gdy build zacznie boleć (Faza 6+).
- 💤 **`public` w `main`** — zostawione konwencjonalnie mimo że Java 25 pozwala usunąć.

---

## Faza 1 — Routing + drugi serwis ⏳
*(jeszcze nierozpoczęta)*

## Faza 2 — Przestrzeń powietrzna ⏳
## Faza 3 — Osiągi i optymalizacja ⏳
## Faza 4 — Bezpieczeństwo ⏳
## Faza 5 — AI ⏳
## Faza 6 — Orkiestracja i async ⏳
## Faza 7 — Chmura, IaC, CI/CD ⏳
## Faza 8+ — Rozszerzanie rdzenia ⏳

---

## Decyzje projektowe (podjęte)
- ✅ **Great-circle: haversine (kula) na start.** MVP najpierw; matematyka trywialna → skupienie
  na strukturze. Docelowo **Karney (GeographicLib)** jako udokładnienie — Vincenty przestarzały
  (problemy zbieżności dla antypodów), Karney to współczesny standard (dokładność nanometrowa).
- ✅ **Jednostka wewnętrzna: metr (SI).** Konwersja na km/NM/mile = warstwa prezentacji.
- ✅ **Nazwy modułów krótkie** (`flight-planning`, bez prefiksu `icarus-`).

## Decyzje otwarte
- 💤 Secure Boot: MOK key signing vs pozostawienie wyłączonego (środowisko OS)
- 💤 Front: React vs Vue (decyzja po Fazie 3)