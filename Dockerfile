FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY src src

RUN ./gradlew installDist

FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && apt-get upgrade -y && apt-get install ca-certificates curl gnupg git -y
RUN mkdir -p /etc/apt/keyrings
RUN curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
RUN echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_21.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list
RUN apt-get update && apt-get install nodejs -y
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

WORKDIR /app

COPY --from=build /app/build /app/build

ENTRYPOINT ["./build/install/libyear-ort/bin/libyear-ort"]
CMD ["--help"]
