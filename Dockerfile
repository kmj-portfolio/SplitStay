# Stage 1 Build

FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle* settings.gradle* gradle.properties* ./
RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon dependencies

COPY src/ src/
RUN ./gradlew --no-daemon -x test bootJar

RUN set -eux; \
    JAR="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"; \
    echo "Using JAR: $JAR"; \
    cp "$JAR" /app/app.jar


# Stage 2 Run

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN set -eux; \
    addgroup --system appgroup; \
    adduser  --system --ingroup appgroup appuser

COPY --from=builder --chown=appuser:appgroup /app/app.jar /app/app.jar

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]