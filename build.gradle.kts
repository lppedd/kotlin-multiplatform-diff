import io.github.petertrr.configurePublishing
import io.github.petertrr.ext.booleanProperty
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.detekt)
    id("jacoco-convention")
}

group = "io.github.petertrr"
description = "A multiplatform Kotlin library for calculating text differences"

dependencies {
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1
    }

    jvm {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    // Minimum bytecode level is 52
                    jvmTarget = JvmTarget.JVM_1_8

                    // Output interfaces with default methods
                    freeCompilerArgs.addAll(
                        "-Xjvm-default=all",      // Output interfaces with default methods
                        "-Xno-param-assertions",  // Remove Intrinsics.checkNotNullParameter
                    )
                }
            }
        }

        testRuns.configureEach {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    js {
        val testConfig: (KotlinJsTest).() -> Unit = {
            useMocha {
                // Override default 2s timeout
                timeout = "120s"
            }
        }

        browser {
            testTask(testConfig)
        }

        nodejs {
            testTask(testConfig)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()

    // Tier 3
    mingwX64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    watchosDeviceArm64()

    // Deprecated.
    // Should follow the same route as official Kotlin libraries
    @Suppress("DEPRECATION")
    linuxArm32Hfp()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

configurePublishing()

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("detekt.yml"))
    autoCorrect = booleanProperty("detektAutoCorrect", default = true)
}

tasks {
    check {
        dependsOn(detekt)
    }
}
