FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY src src

RUN ./gradlew installDist

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=build /app/build /app/build

ENTRYPOINT ["./build/install/libyear-ort/bin/libyear-ort"]
CMD ["--help"]
