#syntax=docker/dockerfile:1
# Powyższa dyrektywa włącza pełne możliwości silnika BuildKit (np.RUN --mount=type=cache)

# ==============================================================================
# ETAP 1: Kompilacja i ekstrakcja warstw (Pełne JDK 25)
# ==============================================================================
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace

# Krok 1a: Selektywne kopiowanie plików definicji projektu i kodu źródłowego modułu.
# Kopiujemy wyłącznie niezbędne ścieżki zamiast całego kontekstu (COPY . .),
# aby zmiany w innych modułach lub skryptach repozytorium nie unieważniały warstw tego serwisu.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY flight-planning/build.gradle.kts ./flight-planning/
COPY flight-planning/src ./flight-planning/src

# Krok 1b: Kompilacja i ekstrakcja warstw za pomocą narzędzia Spring Boot 4.x (-Djarmode=tools).
# - Utrzymanie pobranej dystrybucji Gradle oraz zależności między przebiegami zapewnia
#   wolumen BuildKit (--mount=type=cache). Uwaga dla CI (Faza 7): na efemerycznych agentach CI
#   ten cache wymaga jawnej konfiguracji eksportu (--cache-to / --cache-from).
# - Flaga "-x test" jest świadomą decyzją architektoniczną: testy jednostkowe i integracyjne
#   są uruchamiane przed budowaniem obrazu (lokalnie przed commitem oraz w osobnym etapie potoku CI).
# - Wskazujemy jawnie plik "app.jar" (skonfigurowany przez archiveFileName w build.gradle.kts),
#   unikając podatnego na błędy rozwinięcia globu "*.jar" przez powłokę.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :flight-planning:bootJar --no-daemon -x test && \
    java -Djarmode=tools -jar flight-planning/build/libs/app.jar extract --layers --launcher --destination extracted

# ==============================================================================
# ETAP 2: Docelowy obraz uruchomieniowy (Odchudzone JRE 25 + Non-Root User)
# ==============================================================================
FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app

# Krok 2a: Tworzymy dedykowaną grupę i użytkownika systemowego (UID/GID 10001)
RUN groupadd --system --gid 10001 appgroup && \
    useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin appuser

# Krok 2b: Kopiujemy warstwy z etapu builder w kolejności od najrzadziej do najczęściej zmienianych.
# Ponieważ instrukcja USER jest niżej, skopiowane pliki należą do root:root (0644/0755) —
# proces aplikacji będzie mógł je tylko czytać, ale nie będzie mógł ich nadpisać!
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

# Krok 2c: Przełączamy kontekst na numeryczny identyfikator użytkownika (zgodność z K8s runAsNonRoot)
USER 10001:10001

# Krok 2d: Metadane dokumentujące port nasłuchiwania.
# Świadomy brak HEALTHCHECK: parametry sondowania (interwały, timeouty, ścieżki
# /actuator/health/liveness i /readiness) zależą od środowiska i należą do warstwy orkiestracji.
# Decyzja odłożona do Fazy 1: obraz nie zawiera narzędzi HTTP (curl/wget), więc healthcheck
# w docker-compose wymaga osobnego rozwiązania. W Kubernetesie sondy HTTP wykonuje kubelet.
EXPOSE 8080

# Krok 2e: Uruchomienie w formie tablicowej (Exec form -> proces java staje się PID 1)
ENTRYPOINT [ \
    "java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "org.springframework.boot.loader.launch.JarLauncher" \
]