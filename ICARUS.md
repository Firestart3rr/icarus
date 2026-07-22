# ICARUS

**Integrated Computation of Aerial Routes & Untethered Simulation**

Platforma planowania i analizy lotów dla lotnictwa ogólnego (GA) — ambitny projekt
edukacyjny łączący tło inżyniersko-lotnicze autora (analiza wytrzymałościowa, konstrukcja,
osiągi) z nowoczesnym backendem, architekturą mikroserwisową, infrastrukturą i AI.

> Ten dokument to **całościowy pakiet projektu** — wizja, stack, architektura i roadmapa.
> Bieżący postęp prac śledzony jest osobno w [`PROGRESS.md`](./PROGRESS.md).

---

## 1. Czym jest projekt

System wspierający planowanie tras lotniczych wraz z analizą pogody, przestrzeni powietrznej,
osiągów statku powietrznego, zużycia paliwa i ryzyka lotu.

**Zakres startowy** zawężony do GA i jednego typu statku — **Cessna 172** (dane osiągowe
publicznie dostępne). Kluczowa decyzja projektowa: „typ statku powietrznego" budowany jako
abstrakcja (wzorzec **Strategy** / modele plug-in), tak by później dało się dołożyć business
jeta, uproszczony model dużego odrzutowca i drona/BSP bez przebudowy architektury. Moment
dodania dronów posłuży jako realny test rozszerzalności architektury.

## 2. Filozofia pracy

- **Autor prowadzi projekt samodzielnie.** Claude pełni rolę mentora, konsultanta technicznego,
  recenzenta kodu i pomocy przy refaktoryzacji — **nie pisze projektu za autora, bez vibe
  codingu.**
- **Rozmowy (czaty)** = projektowanie, decyzje architektoniczne, nauka koncepcji, omawianie
  „dlaczego tak". Nowy czat na nową fazę lub większy temat; to podsumowanie służy jako pakiet
  startowy do wklejenia.
- **Claude Code** = praca bezpośrednio przy kodzie w kontekście całego repozytorium (review,
  diagnostyka, refaktoryzacja), w trybie „konsultant, nie wykonawca". Przejście na Claude Code
  do review planowane, gdy kodu przybędzie (orientacyjnie koniec Fazy 1).
- **Świadomy nacisk na warstwę konfiguracyjno-infrastrukturalną** — tu dziś buduje się przewaga
  inżyniera — przy zachowaniu zdrowej proporcji: solidny, nieprzerośnięty kod aplikacyjny +
  głęboko przemyślana infrastruktura wokół niego.
- **Budowa przyrostowa** — wąskie, działające MVP najpierw, potem rozbudowa. Każda technologia
  wchodzi, **gdy realnie rozwiązuje problem, który już istnieje** — nie „na zapas".

## 3. Stack i technologie

| Obszar | Wybór i uzasadnienie |
|---|---|
| **Język / framework** | Java + Spring Boot (aktualna stabilna linia). **Uwaga:** Spring Boot **nie** ma wydań LTS — każda wersja minor ma 12-miesięczne wsparcie OSS, nowa minor co ~pół roku. To *Java* ma LTS-y. |
| **Build** | Gradle z **Kotlin DSL** (świadoma zmiana z Mavena — nowa umiejętność, błyszczy w projekcie wielomodułowym, osłuchanie z Kotlinem). Gradle Wrapper (`./gradlew`) od początku. |
| **Zarządzanie JDK/Gradle** | **SDKMAN!** (nie systemowe apt — mniej niespodzianek z wersjami). |
| **ORM / dostęp do danych** | Hibernate / Spring Data JPA jako domyślny; Hibernate Spatial przy geometrii PostGIS; jOOQ / natywny SQL do trudniejszych zapytań. Umiejętność rozpoznania, kiedy ORM pomaga, a kiedy przeszkadza. |
| **Testy** | JUnit 5 + Mockito + AssertJ + Spring Boot Test (integracyjne) + Testcontainers (prawdziwe bazy w Dockerze na czas testów). Świadome rozróżnianie testów jednostkowych i integracyjnych. |
| **Konteneryzacja / orkiestracja** | Docker → docker-compose (Fazy 1–5, lokalnie) → Kubernetes (od Fazy 6, lokalnie kind/k3d/minikube, potem managed w chmurze). |
| **Komunikacja między serwisami** | REST/gRPC (sync) + Kafka/RabbitMQ (async). API Gateway na wejściu. |
| **AI** | Spring AI; LLM przez API lub lokalnie (Ollama, 0 zł); RAG na PGVector; Python dopiero przy własnym modelu ML — wtedy osobny serwis polyglot w FastAPI. |
| **Front** | Decyzja świadomie odłożona do momentu, gdy backend ma co pokazać (po Fazie 3). Kandydaci: React lub Vue. Estetyka „jak FlightRadar" pochodzi od biblioteki map (MapLibre/Leaflet), nie od frameworka. Do tego czasu backend testowany przez Swagger/OpenAPI + curl/Postman. |
| **Chmura** | Free tier + budget alerts od pierwszego dnia; zasoby włączane tylko na sesje nauki. GCP zwykle najhojniejsze kredyty na start. |
| **System operacyjny** | Ubuntu (dual boot na prywatnym laptopie HP OMEN). |

### Bazy danych (poliglotyzm — wprowadzany przyrostowo)

Zasada **persistence per service** — każdy serwis dobiera bazę pod własne potrzeby.

| Serwis | Baza | Dlaczego |
|---|---|---|
| auth | PostgreSQL | konta, role — strukturalne, wymaga spójności |
| flight-planning | PostgreSQL (+ PostGIS) | zapisane plany, dane geoprzestrzenne |
| weather | Redis (cache) + MongoDB (surowe dane) | szybki odczyt + elastyczna struktura METAR/TAF |
| airspace | PostgreSQL + PostGIS | geometria stref, zapytania przestrzenne |
| risk-ai | MongoDB (+ PGVector do RAG) | raporty AI o zmiennej strukturze + wektory |
| (opcjonalnie) routing | Neo4j | sieć tras/waypointów to dosłownie graf |

**Kolejność wprowadzania:** PostgreSQL na start (przejście z Oracle łagodne — ten sam SQL,
inny dialekt; PostGIS to nowość) → Redis przy cache pogody → MongoDB przy danych zmiennych →
Neo4j jako ambitne rozszerzenie. PGVector = Postgres w nowej roli, bez osobnej bazy.

## 4. Architektura — mikroserwisy rdzenia

1. **flight-planning** — wyznaczanie trasy, waypointy, algorytmy grafowe; orkiestrator wołający pozostałe serwisy
2. **weather** — integracja z METAR/TAF, dane GRIB, warstwa cache
3. **airspace** — strefy powietrzne, NOTAM-y, geometria 3D, sprawdzanie przecięć trasy ze strefami
4. **aircraft-performance** — modele osiągów per typ statku (wzorzec Strategy); tu wraca inżynieria autora
5. **risk-ai** — ocena ryzyka lotu, asystent AI (+ RAG na późniejszym etapie)
6. **auth** — uwierzytelnianie, autoryzacja

### Konwencja nazewnictwa modułów

Krótkie nazwy (`flight-planning`, `weather`, …) bez prefiksu `icarus-`. Repozytorium dostarcza
kontekst nazwy. Prefiks `icarus/` wchodzi świadomie na innej warstwie — przy tagowaniu obrazów
Docker i w rejestrach — nie w nazwie modułu Gradle. (Świadome rozdzielenie „nazwy modułu
w buildzie" od „nazwy artefaktu w rejestrze".)

## 5. Mapa kompetencji — co gdzie ćwiczone

| Obszar | Konkretnie |
|---|---|
| **Algorytmy** | routing grafowy (A*/Dijkstra) z wagami dynamicznymi (wiatr, strefy), geometria obliczeniowa (przecięcia, geometria sferyczna), struktury przestrzenne (quad-tree/R-tree/octree), interpolacja danych pogodowych |
| **Zaawansowane programowanie** | wielowątkowość/współbieżność (wirtualne wątki, CompletableFuture, structured concurrency, thread safety, wyścigi, pule wątków), rekurencja, Stream API, programowanie funkcyjne, wzorce projektowe w praktyce |
| **Optymalizacja** | profil lotu (paliwo vs czas vs ryzyko, front Pareto), cache i inwalidacja, profilowanie pod pomiar, skalowanie podów |
| **Mikroserwisy** | Spring Cloud, service discovery, API gateway, distributed tracing, circuit breaker/retry, komunikacja sync/async |
| **Docker / Kubernetes** | konteneryzacja, deploymenty, service, ingress, configmap/secret, health checki, horizontal pod autoscaling |
| **Cyberbezpieczeństwo** | OAuth2/JWT, mTLS między serwisami, rate limiting, walidacja danych z zewnętrznych API, secrets management, OWASP API Top 10; front: tokeny, XSS, CORS; AI: prompt injection, zatruwanie RAG |
| **Linux / konfiguracja** | powłoka, uprawnienia, procesy, sieć, systemd, logi — świadomy, pogłębiony nacisk |

## 6. Roadmapa — rdzeń (Fazy 0–8)

Każda faza daje coś działającego i wprowadza garść nowych zagadnień. Bezpieczeństwo
i optymalizacja dokładane świadomie warstwami — najpierw budujesz, co chronić, potem chronisz.

- **Faza 0 — fundament.** `flight-planning`, jeden endpoint: dwa punkty → trasa po wielkim
  okręgu (great-circle) + dystans. Nowe: Gradle Kotlin DSL, struktura Spring Boot, pierwszy
  Dockerfile.
- **Faza 1 — pierwszy realny algorytm + drugi serwis.** `weather` (zaślepka lub realny METAR).
  Trasa przestaje być linią prostą → graf waypointów i routing (Dijkstra/A*). Pierwsza
  wielowątkowość: równoległe odpytanie pogody. Nowe: routing grafowy, REST, docker-compose,
  wirtualne wątki. **Tu też: convention plugin w `buildSrc`** (wyniesienie wspólnej konfiguracji
  Gradle, gdy duplikacja stanie się realna).
- **Faza 2 — przestrzeń powietrzna + geometria.** `airspace`. Trasa omija strefy zakazane →
  geometria obliczeniowa, struktury przestrzenne, backtracking. Nowe: algorytmy geometryczne,
  struktury przestrzenne.
- **Faza 3 — osiągi i optymalizacja.** `aircraft-performance` z modelem Cessny 172 + abstrakcja
  typu statku (**Strategy**). Trasa uwzględnia paliwo, czas, zasięg. Pierwsza optymalizacja
  wielokryterialna. Nowe: wzorzec Strategy, optymalizacja, profilowanie. **Po tej fazie backend
  ma realne dane → decyzja o froncie.**
- **Faza 4 — bezpieczeństwo.** `auth` + API Gateway. OAuth2/JWT, rate limiting, walidacja
  wejścia, zabezpieczenie komunikacji między serwisami, przegląd OWASP API Top 10. Nowe:
  OAuth2/JWT, gateway, mTLS, OWASP.
- **Faza 5 — AI.** `risk-ai` — asystent oceny ryzyka (LLM: pogoda + strefy + osiągi →
  rekomendacja go/no-go z uzasadnieniem). Start: Ollama lokalnie lub tanie API. Nowe: Spring AI,
  integracja LLM, projektowanie promptów.
- **Faza 6 — orkiestracja i async.** Przeniesienie całości na Kubernetes lokalnie. Kafka
  (pogoda publikuje aktualizacje asynchronicznie). Health checki, autoscaling. Nowe: Kubernetes,
  Kafka, skalowanie.
- **Faza 7 — chmura, IaC, CI/CD.** Managed Kubernetes, Terraform, GitHub Actions. Dyscyplina
  kosztów. Nowe: chmura, IaC, pipeline CI/CD.
- **Faza 8+ — rozszerzanie rdzenia.** Drugi typ statku (business jet), potem dron/BSP — test
  rozszerzalności architektury.

## 7. Pięć modułów zaawansowanych — gdzie i kiedy

Moduły **rozbudowujące szerokość** (nowe serwisy domenowe — późne fazy):

- **Silnik trajektorii** (osobny serwis) — fizyczny model ruchu: numeryczne całkowanie równań
  ruchu (Runge-Kutta), trajektoria 4D pod wpływem wiatru, masy, faz lotu, zużycia paliwa.
  Najmocniejszy serwis — spotkanie inżynierii z kodem. AI: surrogate models (ML naśladuje silnik
  fizyczny), ewentualnie PINN. Naturalny moment na Pythona (FastAPI + ML).
- **Optymalizacja wielolotowa** (osobny serwis) — harmonogramowanie floty: optymalizacja
  kombinatoryczna, metaheurystyki (algorytmy genetyczne, symulowane wyżarzanie), ewentualnie
  OR-Tools. Konsumuje silnik trajektorii. Po silniku trajektorii (surrogate model przyspiesza
  pętlę optymalizacji).

Moduły **rozbudowujące dojrzałość** (warstwy/wzorce — rozwijane przyrostowo, nie „na koniec"):

- **Observability** — OpenTelemetry (warstwa przekrojowa). Distributed tracing, metryki,
  korelacja logów, dashboardy (Prometheus + Grafana + Jaeger/Tempo). **Zaczynać wcześnie (od
  Fazy 6)** — największa wartość ujawnia się podczas budowy kolejnych rzeczy. AI: wykrywanie
  anomalii (AIOps).
- **Chaos engineering** (praktyka) — celowe psucie systemu (Chaos Mesh / LitmusChaos) dla
  weryfikacji odporności. Po Fazie 6, po observability (żeby widzieć skutek wstrzykniętej
  awarii).
- **Event Sourcing + CQRS** (wzorzec w jednym serwisie) — strumień niezmiennych zdarzeń zamiast
  aktualnego stanu; CQRS rozdziela zapis i odczyt. Punktowo na **flight-planning** (audyt planów
  lotów ma sens). Przy/po Fazie 6 (spina się z Kafką). **Uwaga: łatwo przeinżynierować —
  stosować punktowo.**

## 8. Bonusy / świadomie odłożone rozszerzenia

- **Real-time tracking** (na koniec) — strumień pozycji: Kafka + Cassandra (wide-column, szeregi
  czasowe) + Redis (stan „na żywo") + WebSocket/SSE. Domyka pętlę: plan kontra rzeczywistość.
  Lżejsza alternatywa: TimescaleDB.
- **RAG** (rozszerzenie Fazy 5) — asystent wiedzy lotniczej / wyjaśnialna ocena ryzyka na
  PGVector.
- **Micronaut/Quarkus** (Faza 6+) — przepisanie jednego serwisu dla porównania czasu startu
  i pamięci (+ GraalVM Native).
- **Neo4j** — routing oparty o graf.

## 9. Wzorce projektowe — stały wątek nauki

**Zasada nadrzędna: wzorzec przychodzi z problemu, nie problem z wzorca.** Dobry wzorzec jest
niewidoczny. Metoda pracy: najpierw rozwiązanie „wprost", potem — gdy pojawi się ból (duplikacja,
sztywność, puchnący if-else) — refaktor do wzorca ze zrozumieniem *dlaczego*.

**Dawane przez Spring „za darmo" (warto rozpoznawać):** Singleton (beany), Dependency Injection /
IoC (zastępuje część wzorców twórczych GoF), Proxy (`@Transactional`, security), Template Method
(klasy `...Template`).

**Pisane samodzielnie, gdy faza zażąda:** Strategy (typ statku, Faza 3 — gwiazda projektu;
realizowany przez wstrzykiwanie `List`/`Map` beanów), Repository (Faza 1+), Builder (obiekty
domenowe; uwaga na `record`y Javy 25, które zastępują część potrzeb), Factory (często zwija się
do DI), Observer (Kafka, Faza 6), Command + Event Sourcing/CQRS (Faza 6+), Circuit Breaker
(odporność), Adapter (integracja zewnętrznych API pogodowych).

**Materiały:** GoF (posiadany, kanoniczny ale z 1994 — czytany dosłownie prowadzi do
przeinżynierowania). Rekomendowane uzupełnienie: *Head First Design Patterns* (wyd. 2, 2020 —
„kiedy i dlaczego", Java) i *Refactoring* (Fowler — dochodzenie do designu małymi krokami).
Papier ma sens dla wzorców (wiedza stabilna). Odwrotnie dla szybkozmiennych rzeczy (np. Spring
Boot 4 — tu dokumentacja online, nie papier).

## 10. Koszty i materiały

Większość projektu wykonalna za **0 zł** (lokalnie: Java, Spring, Docker, K8s przez kind/k3d,
bazy w kontenerach, Kafka, Ollama). Dane lotnicze/pogodowe w dużej części publiczne. Chmura
~0–100 zł/mies. przy dyscyplinie (wyłączanie zasobów, budget alerty, kredyty). AI: lokalnie 0 zł
lub kilka–kilkanaście zł/mies. przez API. Sekurak (bezpieczeństwo), Udemy/Pluralsight (Spring
Cloud, K8s, chmura) — kupowane pod aktualny etap, ze świadomością że materiały bywają jeszcze pod
SB 3.x.

---

## Metadane projektu

| | |
|---|---|
| **Group** | `io.github.firestart3rr` |
| **Artifact** | `icarus` |
| **Package** | `io.github.firestart3rr.icarus` |
| **Java** | 25 (LTS, Temurin via SDKMAN) |
| **Spring Boot** | 4.1.0 |
| **Build** | Gradle (Kotlin DSL), multi-module monorepo |
| **Repo** | GitHub — prywatne konto autora |