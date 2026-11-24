import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val windowsRuntime: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.runtimeClasspath.get())
    val rcAttrs = configurations.runtimeClasspath.get().attributes
    attributes {
        for (key in rcAttrs.keySet()) {
            @Suppress("UNCHECKED_CAST")
            attribute(key as Attribute<Any>, rcAttrs.getAttribute(key) as Any)
        }
    }
}

val linuxRuntime: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.runtimeClasspath.get())
    val rcAttrs = configurations.runtimeClasspath.get().attributes
    attributes {
        for (key in rcAttrs.keySet()) {
            @Suppress("UNCHECKED_CAST")
            attribute(key as Attribute<Any>, rcAttrs.getAttribute(key) as Any)
        }
    }
}

dependencies {
    api(project(":shared-app"))
    implementation(project(":core-digital"))
    implementation(project(":core-bridge"))
    implementation(project(":core-data"))
    implementation(project(":core-designsystem"))
    implementation(project(":feature-breadboard"))
    implementation(project(":feature-instruments"))
    implementation(project(":feature-tools"))

    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.animation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.slf4j.nop)

    // Platform-specific x64 runtime configurations for Windows and Linux Mint bundles
    windowsRuntime(compose.desktop.windows_x64)
    linuxRuntime(compose.desktop.linux_x64)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.logiclabs.app.MainKt"

        jvmArgs(
            "-Dsun.java2d.dpiaware=true",
            "-Dskiko.vsync.enabled=true"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Logic Labs"
            packageVersion = "1.1.3"
            description = "Digital Electronics Lab & K&H IDL-800A Breadboard Simulator for Windows and Linux"
            vendor = "Logic Labs"
            copyright = "GPL-3.0-only"
            licenseFile.set(rootProject.file("LICENSE"))

            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.prefs",
                "jdk.unsupported"
            )

            windows {
                menu = true
                menuGroup = "Logic Labs"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8f4b2a19-5c3d-4e92-a7b1-3d2c9e6f1a08"
                val ico = project.file("src/main/resources/icon.ico")
                if (ico.exists()) {
                    iconFile.set(ico)
                }
            }

            linux {
                packageName = "logic-labs"
                debPackageVersion = "1.1.3"
                menuGroup = "Education;Electronics;Development"
                shortcut = true
                val png = project.file("src/main/resources/icon.png")
                if (png.exists()) {
                    iconFile.set(png)
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    System.getProperty("logiclabs.captureScreenshots")?.let {
        systemProperty("logiclabs.captureScreenshots", it)
    }
}

fun registerPlatformFatJar(
    platformKey: String,
    archiveBase: String,
    runtimeConfig: Configuration,
    excludedNativeSubstrings: List<String>
) {
    val mergedDir = layout.buildDirectory.dir("tmp/${platformKey}FatJarServices")

    val mergeTask = tasks.register("merge${platformKey.replaceFirstChar { it.uppercase() }}Services") {
        dependsOn(configurations.runtimeClasspath, runtimeConfig)
        outputs.dir(mergedDir)
        doLast {
            val outDir = mergedDir.get().asFile
            outDir.deleteRecursively()
            val services = linkedMapOf<String, LinkedHashSet<String>>()
            val files = runtimeConfig.files.filter { file ->
                val n = file.name.lowercase()
                excludedNativeSubstrings.none { n.contains(it) }
            }
            for (jarFile in files) {
                if (!jarFile.exists() || !jarFile.isFile || !jarFile.name.endsWith(".jar")) continue
                ZipFile(jarFile).use { zf ->
                    val entries = zf.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && entry.name.startsWith("META-INF/services/")) {
                            val set = services.getOrPut(entry.name) { linkedSetOf() }
                            zf.getInputStream(entry).bufferedReader().useLines { lines ->
                                lines.map { it.trim() }
                                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                                    .forEach { set.add(it) }
                            }
                        }
                    }
                }
            }
            for ((path, impls) in services) {
                val target = File(outDir, path)
                target.parentFile.mkdirs()
                target.writeText(impls.joinToString("\n") + "\n")
            }
        }
    }

    tasks.register<Jar>("${platformKey}FatJar") {
        group = "distribution"
        description = "Assembles a $platformKey x64 fat JAR with Skiko $platformKey native runtime"
        archiveBaseName.set(archiveBase)
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        isZip64 = true

        manifest {
            attributes["Main-Class"] = "com.logiclabs.app.MainKt"
        }

        dependsOn(mergeTask, configurations.runtimeClasspath, runtimeConfig)
        from(mergedDir)
        from(sourceSets.main.get().output)

        from({
            runtimeConfig
                .filter { file ->
                    val n = file.name.lowercase()
                    excludedNativeSubstrings.none { n.contains(it) }
                }
                .map { if (it.isDirectory) it else zipTree(it) }
        }) {
            exclude(
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/MANIFEST.MF",
                "META-INF/services/**",
                "module-info.class",
                "META-INF/versions/*/module-info.class"
            )
        }
    }
}

registerPlatformFatJar(
    platformKey = "windows",
    archiveBase = "LogicLabs-windows",
    runtimeConfig = windowsRuntime,
    excludedNativeSubstrings = listOf("skiko-awt-runtime-linux", "skiko-awt-runtime-macos")
)

registerPlatformFatJar(
    platformKey = "linux",
    archiveBase = "LogicLabs-linux",
    runtimeConfig = linuxRuntime,
    excludedNativeSubstrings = listOf("skiko-awt-runtime-windows", "skiko-awt-runtime-macos")
)
