package com.logiclabs.app.di

import com.logiclabs.core.data.persistence.LogicLabsStorage
import com.logiclabs.core.data.persistence.ProjectsRepository
import com.logiclabs.core.data.persistence.createDefaultStorage
import com.logiclabs.core.data.progress.ProgressRepository
import com.logiclabs.core.data.session.SessionRepository
import com.logiclabs.core.data.settings.SettingsRepository

/**
 * Multiplatform dependency graph shared across Linux, Windows, macOS, and Web (WasmGC / JS).
 */
class AppContainer(
    val storage: LogicLabsStorage = createDefaultStorage()
) {
    val isPortableMode: Boolean get() = storage.isPortableMode
    val storageLocationDisplay: String get() = storage.locationDisplay
    val settings: SettingsRepository = SettingsRepository(storage)
    val session: SessionRepository = SessionRepository(storage)
    val progress: ProgressRepository = ProgressRepository(storage)
    val projects: ProjectsRepository = ProjectsRepository(storage)
}
