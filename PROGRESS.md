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

### Projekt i struktura
- ✅ Wygenerowany skeleton przez Spring Initializr
    - Gradle Kotlin DSL, Java 25, Spring Boot 4.1.0 (stabilny, nie SNAPSHOT), Jar, YAML
    - Group `io.github.firestart3rr`, Artifact `icarus`
    - Dependencies: **Spring Web (webmvc), Actuator, Validation** — świadome minimum, bez bazy
- ✅ Rozbiór `build.gradle.kts` linia po linii (zrozumienie zamiast kopiowania z dokumentacji)
    - zrozumiany mechanizm: `io.spring.dependency-management` + BOM → brak numerów wersji
    - zrozumiana modularyzacja SB4: `-webmvc`, `-actuator`, `-validation` + startery `-test`
- ✅ `./gradlew bootRun` — aplikacja startuje, Tomcat na 8080, Java 25 potwierdzona
- ✅ Weryfikacja `/actuator/health` → `{"status":"UP"}`, grupy `liveness`/`readiness`
  (gotowe pod K8s probes w Fazie 6)
- ✅ GeoPoint (value object, record, walidacja zakresów + NaN/Infinity, testy)

### Kontrola wersji
- ✅ `git init`, `.gitignore` zweryfikowany (build/, .gradle, .idea wykluczone)
- ✅ Tożsamość Git (`user.email`, `user.name`)
- ✅ Klucz SSH ed25519 wygenerowany i dodany do GitHub
- ✅ Repo `icarus` na GitHub (puste, bez auto-inicjalizacji), pierwszy push
- ✅ Commit struktury monorepo + push

### Transformacja w monorepo
- ✅ Utworzenie modułu `flight-planning/`, przeniesienie `src/` (Git wykrył rename → historia zachowana)
- ✅ `settings.gradle.kts` → `include("flight-planning")`
- ✅ **Kluczowa lekcja Maven→Gradle:** blok `dependencies` parenta NIE jest dziedziczony przez
  moduły; każdy moduł ma własny, izolowany classpath
- ✅ Rozdział odpowiedzialności: parent chudy (`group`, `version`, `repositories`); moduł ma
  Spring Boot plugin + startery + toolchain Java 25 (Szkoła A — jawność przy jednym module)
- ✅ Usunięta duplikacja JUnita (startery `-test` dostarczają JUnit spójny ze Spring Boot BOM)
- ✅ `./gradlew :flight-planning:build` — zielony

### Decyzje projektowe
- ✅ **Great-circle: haversine (kula) na start.** Uzasadnienie: biała karta, MVP najpierw;
  matematyka trywialna (jeden wzór, brak iteracji/przypadków brzegowych) → skupienie na
  strukturze, nie na algorytmie. Vincenty (elipsoida) świadomie odłożone jako przyszłe
  udokładnienie, gdy platforma nabierze realizmu.

### W toku / następne
- 🔄 **Endpoint great-circle** — decyzja domenowa: haversine (kula) vs Vincenty (elipsoida)
- ⏳ Struktura pakietów (granice domenowe)
- ⏳ Kod: model punktu, serwis liczący, kontroler REST
- ⏳ Walidacja wejścia (zakresy współrzędnych)
- ⏳ Test jednostkowy serwisu + test kontrolera
- ⏳ Pierwszy Dockerfile

### Dług techniczny (świadomie odłożony)
- 💤 **Convention plugin w `buildSrc`** — wyniesienie wspólnej konfiguracji Gradle
  (`group`/`version`, Spring Boot plugin, `repositories`). Do zrobienia w **Fazie 1**, gdy dojdzie
  `weather` i duplikacja stanie się realna.
- 💤 **Configuration cache** — przyspieszenie buildów. Do włączenia, gdy build zacznie boleć
  (orientacyjnie Faza 6+).

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

## Decyzje otwarte (do rozstrzygnięcia)
- 💤 Secure Boot: MOK key signing vs pozostawienie wyłączonego (środowisko OS)
- 💤 Front: React vs Vue (decyzja po Fazie 3)