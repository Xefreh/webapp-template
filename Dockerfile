# syntax=docker/dockerfile:1

# ===== Build stage =====
# The JDK matches the Gradle toolchain (Java 25), so Gradle uses it instead of downloading one.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Gradle wrapper and build scripts first, so the Gradle distribution and dependencies
# stay cached while only the sources change.
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY app/build.gradle.kts app/
COPY app/src/main/frontend/package.json app/src/main/frontend/package-lock.json app/src/main/frontend/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:dependencies :app:npmInstall > /dev/null

COPY app/src/ app/src/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:shadowJar \
    && cp app/build/libs/app-all.jar /workspace/app.jar

# ===== Runtime stage =====
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /workspace/app.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
