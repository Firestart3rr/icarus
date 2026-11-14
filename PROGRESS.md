# ICARUS — Log postępu

Bieżący rejestr wykonanej pracy. Aktualizowany na bieżąco przez autora.
Pełny opis projektu: [`ICARUS.md`](./ICARUS.md).

**Legenda:** ✅ zrobione · 🔄 w toku · ⏳ zaplanowane · 💤 dług techniczny (świadomie odłożone)

---

## Faza 0 — Fundament ✅

**Cel:** `flight-planning`, jeden endpoint: dwa punkty (start, cel) → trasa po wielkim okręgu
(great-circle) + dystans. Bezstanowo (bez bazy). Pierwszy Dockerfile.

**Wynik:** `POST /api/v1/routes/distance` działa lokalnie i w kontenerze; kontrakt sukcesu
i błędów zablokowany testami; obraz wieloetapowy, nieuprzywilejowany, z łagodnym zamknięciem.

### Środowisko i narzędzia
- ✅ Ubuntu (dual boot) na HP OMEN — środowisko developerskie
- ✅ SDKMAN! + Java 25 Temurin
- ✅ Gradle (przez wrapper, `./gradlew`)
- ✅ IntelliJ IDEA Ultimate
- ✅ Docker z BuildKit
- ✅ Python 3 (systemowy) — do generowania danych referencyjnych w testach
- ✅ Asystenci nauki: **Profesor** (matematyka, fizyka, geodezja), **Profesor Informatyki**
  (jak działa komputer: JVM, sieci, bazy, Docker, algorytmy); decyzje projektowe i review — mentor

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
- ✅ **`DistanceUnit`** (pakiet `domain`) — enum z jedną wartością `METERS`; jednostka to pojęcie
  domenowe, nie webowe; kolejne wartości dochodzą, gdy będą potrzebne
- ✅ **`HaversineDistanceCalculator`** (pakiet `distance`) — `@Service`, bezstanowy
  - świadomie BEZ interfejsu (jedna implementacja; interfejs + Strategy dopiero przy Karneyu)
  - bean, nie metody statyczne — żeby nie zamknąć drogi do przyszłego polimorfizmu
  - bezstanowość = thread-safe z definicji (ważne pod wielowątkowość Fazy 1)
  - clamp `a` do [0,1] — zabezpieczenie przed `NaN` z błędu zaokrągleń float
  - `Math.atan2(√a, √(1−a))` — stabilniejsze niż `asin`
- ✅ **Testy kalkulatora** — mocne, oparte na właściwościach:
  - niezmienniki: zero dla identycznych punktów, symetria d(A,B)=d(B,A)
  - **wartości analityczne** (dowodliwe: π·R, π/2·R) — ćwiartki i pół obwodu, tolerancja 0,1 m
  - **Golden Master** — trasy (WAW→LON, JFK→CDG) policzone niezależnie w Pythonie tym samym
    modelem i R; zgodność Java↔Python do **0,0001 m** → dowód poprawności implementacji

### Warstwa web (pakiet `web`)
- ✅ **Kontrakt API:** `POST /api/v1/routes/distance`
  - żądanie: `{"start":{"latitude","longitude"},"end":{...}}`; odpowiedź: `{"distance","unit"}`
  - POST, bo złożone dane wejściowe jadą w ciele żądania; prefiks `/v1` od początku
  - styl hybrydowy (RPC pod zasobem `/routes`) — świadomie; czysty REST (trasa jako zasób z ID,
    `201 Created` + `Location`) wróci, gdy trasa będzie stanowa (Faza 1)
- ✅ **DTO jako recordy** (`web.dto`): `PointDto`, `RouteRequest`, `RouteResponse`
  - `PointDto` z polami `Double` (obiektowy!) — brak pola w JSON → `null` → `@NotNull` to łapie;
    prymityw `double` zamieniłby brak pola na cichy `0.0` (równik)
  - `@Valid` na zagnieżdżonych polach `RouteRequest` — bez tego walidacja nie schodzi w głąb
  - **jawne `message` przy każdej adnotacji** — kontrakt nie zależy od locale systemu
    (domyślne komunikaty Hibernate Validator tłumaczą się wg locale: PL na laptopie, EN w kontenerze)
- ✅ **`RouteController`** — cienki: tłumaczy DTO → `GeoPoint`, deleguje do kalkulatora,
  pakuje `RouteResponse`; wstrzykiwanie przez konstruktor (`final`), `@Valid @RequestBody`,
  `ResponseEntity`; ścieżki w adnotacjach (bez klasy `ControllerMapping`)
- ✅ **Obsługa błędów — `ApiExceptionHandler`** (rozszerza `ResponseEntityExceptionHandler`)
  - format **RFC 9457 Problem Details** (`application/problem+json`) jako jawna część kontraktu
  - walidacja → 400 + `invalidParams[]` (`field` z pełną ścieżką, np. `start.latitude`; `reason`)
  - nieczytelne ciało (zły typ, uszkodzony JSON) → 400 z domyślnej obsługi, bez wycieku
    komunikatu Jacksona
  - catch-all `Exception` → **500** z ogólnym `detail` i `incidentId`; pełny wyjątek ze stack
    trace'em w logu (ERROR) z tym samym `incidentId`
  - `IllegalArgumentException` celowo NIE mapowany na 400 — nasze bugi nie mogą udawać winy klienta
- ✅ **Testy kontrolera** — `@WebMvcTest` + `@MockitoBean` (kalkulator jako mock)
  - happy path + `verify` mapowania DTO → `GeoPoint` (działa dzięki `equals` recordu)
  - zakres, brak pola, wiele błędów naraz (`containsInAnyOrder` — kolejność naruszeń nie jest
    gwarantowana), NaN odrzucany przez walidację, nieczytelne ciało (brak `invalidParams`)
  - 500: brak wycieku komunikatu wyjątku w całym ciele odpowiedzi, obecny `incidentId`
  - `verifyNoInteractions` w testach 400 — dowód, że brama odcina żądanie przed domeną
  - URL wpisany literalnie (test kontraktu nie korzysta ze stałych z kodu)

### Konteneryzacja
- ✅ **Dockerfile wieloetapowy**
  - builder: `eclipse-temurin:25-jdk-noble`; runtime: `eclipse-temurin:25-jre-noble`
  - cache Gradle przez `RUN --mount=type=cache` (BuildKit); `-x test` świadomie (testy przed budową)
  - `bootJar` → stała nazwa `app.jar`, zadanie `jar` wyłączone (bez `-plain.jar`)
  - warstwy wypakowane `-Djarmode=tools extract --layers --launcher`
  - użytkownik `10001:10001` (numerycznie — zgodność z K8s `runAsNonRoot`); pliki aplikacji
    należą do roota → proces może je tylko czytać
  - `ENTRYPOINT` w formie exec (`java` jako PID 1), `-XX:MaxRAMPercentage=75.0`,
    `-XX:+ExitOnOutOfMemoryError`
  - bez `HEALTHCHECK` — decyzja odłożona do Fazy 1 (obraz nie ma curl/wget)
- ✅ **`.dockerignore`** z wzorcami `**/build/`, `**/.gradle/` (semantyka inna niż `.gitignore`)
- ✅ **Weryfikacja w kontenerze:** happy path i komunikaty walidacji identyczne jak lokalnie;
  `id` → `uid=10001 gid=10001`
- ✅ **Eksperyment PID 1:**

  | | `java` jako PID 1 (exec form) | `sh` jako PID 1 |
  |---|---|---|
  | czas `docker stop` | 0,23 s | 10,2 s |
  | kod wyjścia | 143 (`SIGTERM`) | 137 (`SIGKILL`) |
  | łagodne zamknięcie w logach | tak | brak |

### Lekcje warte zapamiętania (z tej fazy)
- **Model domenowy ≠ DTO** — rozdzielamy, bo zmieniają się z różnych powodów (domena vs kontrakt
  API), nie tylko „bo w różnych pakietach"
- **Nazwa pakietu = rola w domenie, nie technika** (`distance`, `domain` — nie `utils`/`algorithms`)
- **Value object → record; encja → klasa** (tożsamość vs zawartość)
- **LLM: dobry do wyjaśnień pojęć, ZŁY do danych liczbowych** — asystent podał „wartości
  referencyjne" i etykietę „z PostGIS" bez uruchomienia narzędzia; prawdziwy Golden Master
  wygenerowany samodzielnie (analitycznie + Python). Rygor wpisany w prompty obu asystentów.
- **Kto musi coś zmienić?** 4xx — klient poprawia żądanie; 5xx — my poprawiamy kod
- **Przewiduj ścieżkę, nie tylko status** — 400 z walidacji i 400 z parsowania to dwie różne drogi
- **Cisza nie jest dowodem** — brak wyjścia czy brak błędu sprawdza się kodem wyjścia, logiem, `-i`
- **Łagodne zamknięcie dowodzą trzy rzeczy:** wpis w logu, czas zatrzymania, kod wyjścia (143)
- **Czytaj komunikat dokładnie:** `connection refused` (nikt nie nasłuchuje) ≠ `connection reset`
  (połączenie przyjęte i zerwane — np. zły port w `-p HOST:KONTENER`)
- **Ręczny curl jest nieodtwarzalny** — kontrakt zamyka się w testach, nie w historii terminala
- **Komentarz lub nazwa, które kłamią, są gorsze niż ich brak** (testy, Dockerfile)
- **Rozmiar obrazu:** CONTENT SIZE (skompresowany) ≠ DISK USAGE; kod aplikacji to 131 kB z ~350 MB

### Dług techniczny (świadomie odłożony)
- 💤 **Convention plugin w `buildSrc`** — wspólna konfiguracja Gradle (`group`/`version`, Spring
  Boot plugin, `repositories`). Do zrobienia w **Fazie 1**, gdy dojdzie `weather`.
- 💤 **Healthcheck kontenera bez curl/wget** — compose wykonuje sondę wewnątrz kontenera;
  wybrać rozwiązanie w **Fazie 1** (w K8s sondy HTTP wykonuje kubelet)
- 💤 **Tag obrazu ręczny (`0.1.0`) vs wersja aplikacji (`0.0.1-SNAPSHOT`)** — tag z wersji
  lub SHA commita; razem z CI w **Fazie 7**
- 💤 **Przypięcie obrazów bazowych po digeście** (`@sha256:...`) — tag `25-jre-noble` przesuwa się
  między wersjami poprawkowymi (lokalnie 25.0.3, w kontenerze 25.0.5); **Faza 7**
- 💤 **Wyjątki Spring Security a catch-all 500** — w **Fazie 4** osobna obsługa, żeby 401/403
  nie zamieniały się w 500
- 💤 **Ostrzeżenie JVM przy testach** (`Sharing is only supported for boot loader classes...`) —
  najpewniej agent Mockito; niegroźne
- 💤 **Configuration cache** — przyspieszenie buildów. Gdy build zacznie boleć (Faza 6+).
- 💤 **`public` w `main`** — zostawione konwencjonalnie mimo że Java 25 pozwala usunąć.

---

## Faza 1 — Routing + drugi serwis ⏳

### Punkt startowy
- Stan wyjściowy: Faza 0 zamknięta — jeden serwis `flight-planning` w monorepo Gradle, kontrakt
  REST z Problem Details, testy jednostkowe i kontrolera, obraz Docker
- Zakres z roadmapy: serwis `weather` (zaślepka lub realny METAR), graf waypointów i routing
  (Dijkstra/A*), równoległe odpytywanie pogody (wirtualne wątki), komunikacja REST między
  serwisami, docker-compose
- Do rozstrzygnięcia na starcie: convention plugin w `buildSrc` (dług z Fazy 0), healthcheck
  w compose, trasa jako zasób REST (model z ID i `201 Created`)

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
  Dystans w API jako `double` (pełna precyzja; zaokrąglanie to sprawa prezentacji).
- ✅ **Nazwy modułów krótkie** (`flight-planning`, bez prefiksu `icarus-`).
- ✅ **Format błędów: RFC 9457 Problem Details**, jeden kształt odpowiedzi (lista `invalidParams`
  zawsze, nawet z jednym elementem), camelCase spójnie z resztą API.
- ✅ **Testy kontrolera z mockiem serwisu** — test warstwy web sprawdza kontrakt i mapowanie;
  poprawność matematyki dowodzą testy kalkulatora.
- ✅ **Obraz: multi-stage, JRE w runtime, użytkownik nieuprzywilejowany, exec form.**

## Decyzje otwarte
- 💤 Secure Boot: MOK key signing vs pozostawienie wyłączonego (środowisko OS)
- 💤 Front: React vs Vue (decyzja po Fazie 3)
