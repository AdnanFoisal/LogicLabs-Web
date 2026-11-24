package com.logiclabs.app.di

import com.logiclabs.core.data.persistence.DesktopAppPaths
import com.logiclabs.core.data.persistence.FileLogicLabsStorage
import java.io.File

/** JVM convenience factory preserving `AppContainer(dataDir: File)` for Desktop entry points and tests. */
fun AppContainer(dataDir: File): AppContainer = AppContainer(FileLogicLabsStorage(dataDir))

val AppContainer.dataDir: File
    get() = (storage as? FileLogicLabsStorage)?.dataDir ?: DesktopAppPaths.resolveDataDir()
