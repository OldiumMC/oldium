import net.fabricmc.loom.task.RemapJarTask

buildscript.repositories.gradlePluginPortal()

plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
    id("legacy-looming") version "1.5-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


group = "dev.mappings"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://raw.githubusercontent.com/BleachDev/cursed-mappings/main/")
}

val modImplementation = configurations.modImplementation.get()
configurations.implementation.get().extendsFrom(modImplementation)

dependencies {
    "minecraft"("com.mojang:minecraft:1.8.9")
    "mappings"("net.legacyfabric:yarn:1.8.9+build.535:v2")
    "modImplementation"("net.fabricmc:fabric-loader:0.15.9")

    modImplementation("org.joml:joml:1.10.5")
    modImplementation("it.unimi.dsi:fastutil:8.5.6")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}

tasks.shadowJar {
    minimize()
    configurations.clear()
    configurations.add(modImplementation)
}

tasks.processResources {
    inputs.property("version", project.version.toString())
}

val remapShadowedJar by tasks.creating(RemapJarTask::class) {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    archiveFileName = "oldium-fabric-1.8.9-${version}.jar"
}

loom {
    accessWidenerPath = file("src/main/resources/oldium.accesswidener")
}

java {
    toolchain {
    languageVersion = JavaLanguageVersion.of(8)
    }
}
