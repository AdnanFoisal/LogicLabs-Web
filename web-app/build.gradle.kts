import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "logic-labs-wasm"
        browser {
            commonWebpackConfig {
                outputFileName = "logic-labs-wasm.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(project.projectDir.path)
                    }
                }
            }
        }
        binaries.executable()
    }

    js(IR) {
        moduleName = "logic-labs-js"
        browser {
            commonWebpackConfig {
                outputFileName = "logic-labs-js.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared-app"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
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

tasks.withType<BinaryenExec>().configureEach {
    binaryenArgs = mutableListOf(
        "--enable-gc",
        "--enable-reference-types",
        "--enable-exception-handling",
        "--enable-bulk-memory",
        "--enable-nontrapping-float-to-int",
        "--traps-never-happen",
        "-O1"
    )
}

/**
 * Stitches the WasmGC primary distribution (`wasm/`), JS IR fallback distribution (`js/`),
 * smart WasmGC-probing `index.html`, bundled fonts, PWA manifest/icons, offline Service Worker,
 * and Cloudflare `_headers` into `web-app/build/dist/combined/` ready for GitHub Pages or Cloudflare Pages.
 */
tasks.register<Sync>("assembleCombinedWebDist") {
    group = "distribution"
    description = "Builds both wasmJs and js production distributions into one dual-engine offline PWA site"

    dependsOn("wasmJsBrowserDistribution", "jsBrowserDistribution")

    val combinedDir = layout.buildDirectory.dir("dist/combined")
    into(combinedDir)

    // Root static web shell (index.html, manifest.webmanifest, sw.js, _headers, .nojekyll, icons, fonts)
    from(layout.projectDirectory.dir("src/webMain/resources"))

    // Bundled Inter & JetBrains Mono fonts for both Compose UI and Skia Canvas
    from(rootProject.file("core-designsystem/src/jvmMain/resources/font")) {
        into("font")
    }

    // Fast WasmGC production bundle (logic-labs-wasm.js, logic-labs-wasm.wasm, hashed skiko wasm)
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
        exclude("index.html", "manifest.webmanifest", "sw.js", "_headers", ".nojekyll", "favicon.png", "icons/**", "font/**", "*.map")
    }

    // Automatic JS IR fallback bundle (skiko.js, skiko.wasm, logic-labs-js.js)
    from(layout.buildDirectory.dir("dist/js/productionExecutable")) {
        exclude("index.html", "manifest.webmanifest", "sw.js", "_headers", ".nojekyll", "favicon.png", "icons/**", "font/**", "*.map")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
