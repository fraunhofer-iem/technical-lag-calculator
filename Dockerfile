# Use a multi-stage build for smaller final image size
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy only necessary files for dependency resolution and build
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY src src

# Build the application
RUN ./gradlew installDist

# Stage 2: Create the production image
FROM eclipse-temurin:21-jdk-jammy

# Install necessary packages
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    gnupg \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js and Yarn
RUN curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /usr/share/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/nodesource.gpg] https://deb.nodesource.com/node_21.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list && \
    apt-get update && \
    apt-get install -y nodejs && \
    npm install -g yarn && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

RUN curl -fsSL https://get.pnpm.io/install.sh | sh -

RUN git config --global --add safe.directory '*'

WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/build /app/build

# Set the entrypoint and default command
ENTRYPOINT ["./build/install/libyear-ort/bin/libyear-ort"]
CMD ["--help"]
