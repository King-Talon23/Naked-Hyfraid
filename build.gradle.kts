plugins {
    java
}

group = "KingTalon23"
version = "0.1.0"

repositories {
    mavenCentral()
    // Official Hytale server API repository: https://hytalemodding.dev/en/docs/guides/plugin/setting-up-env
    maven("https://maven.hytale.com/release")
}

dependencies {
    // Pin this to the server build you run (e.g. "2026.01.17-4b0f30090") for reproducible builds.
    compileOnly("com.hypixel.hytale:Server:+")
}

java {
    toolchain {
        // The Hytale server runs on Java 25.
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.jar {
    archiveBaseName.set("NakedHyfraid")
}
