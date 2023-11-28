import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

group = "iem.fraunhofer.de"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven("https://androidx.dev/storage/compose-compiler/repository")
        }

        filter {
            includeGroup("androidx.compose.compiler")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.gradle.org/gradle/libs-releases/")
        }

        filter {
            includeGroup("org.gradle")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.8"
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.3.0")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.21.1")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.ossreviewtoolkit:analyzer:6.1.1")
    implementation("org.ossreviewtoolkit:model:6.1.1")
    implementation("org.ossreviewtoolkit:reporter:6.1.1")
    implementation("org.ossreviewtoolkit.plugins.packagecurationproviders:package-curation-provider-api:6.1.1")
    implementation("org.ossreviewtoolkit.plugins.packagemanagers:maven-package-manager:6.1.1")
    implementation("org.ossreviewtoolkit.plugins.packagecurationproviders:ort-config-package-curation-provider:6.1.1")
    implementation("org.ossreviewtoolkit.plugins.packagecurationproviders:clearly-defined-package-curation-provider:6.1.1")
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-crypt:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.44.1")
    implementation("org.jetbrains.exposed:exposed-json:0.44.1")
    implementation("org.jetbrains.exposed:exposed-money:0.44.1")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.44.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    implementation("io.ktor:ktor-client-core-jvm:2.3.6")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
    runtimeOnly("org.postgresql:postgresql:42.6.0")
    runtimeOnly("org.ossreviewtoolkit.plugins.packagecurationproviders:file-package-curation-provider:6.1.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.10")
}

tasks.test {
    useJUnitPlatform()
}


application {
    mainClass.set("MainKt")
}