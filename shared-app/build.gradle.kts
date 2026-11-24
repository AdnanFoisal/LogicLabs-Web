import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core-bridge"))
                api(project(":core-data"))
                api(project(":core-designsystem"))
                api(project(":feature-breadboard"))
                api(project(":feature-instruments"))
                api(project(":feature-tools"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.animation)
                api(compose.material3)
                api(compose.ui)
                api(libs.kotlinx.coroutines.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                api(compose.desktop.common)
            }
        }
        val webMain by creating {
            dependsOn(commonMain)
        }
        val wasmJsMain by getting {
            dependsOn(webMain)
        }
        val jsMain by getting {
            dependsOn(webMain)
        }
    }
}
