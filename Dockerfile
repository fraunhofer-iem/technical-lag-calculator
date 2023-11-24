FROM gradle:8.4-jdk17-jammy AS build

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY src src

RUN ./gradlew installDist

FROM gradle:8.4-jdk17-jammy

WORKDIR /app

COPY --from=build /app/build /app/build

ENTRYPOINT ["./build/install/libyear-ort/bin/libyear-ort"]
CMD ["--help"]
