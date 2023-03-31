plugins {
    kotlin("multiplatform") version "1.8.20"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "dev.racci"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {

    explicitApi()

    targets.all {
        compilations.all {
            kotlinOptions {
                apiVersion = "1.6"
                languageVersion = "1.6"
            }
        }
    }

    for (target in listOf(linuxX64(), mingwX64())) {
        target.apply {
            compilations["main"].enableEndorsedLibs = false
            binaries {
                executable {
                    entryPoint = "main"
                }
            }
        }
    }

    sourceSets {

        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
                implementation("com.soywiz.korlibs.korio:korio:2.4.10")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
            }
        }
        val linuxX64Main by getting { dependsOn(commonMain) }
        val mingwX64Main by getting { dependsOn(commonMain) }
    }
}
