package com.logiclabs.core.data.settings

import com.logiclabs.core.bridge.platform.ioDispatcher
import com.logiclabs.core.data.persistence.LogicLabsStorage
import com.logiclabs.core.data.persistence.createDefaultStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How the bench is lit across the 9-Theme Hardware Suite (+ SYSTEM).
 */
enum class ThemeMode {
    SYSTEM,
    OBSIDIAN,
    AMBER_CRT,
    HP_SLATE,
    CLEANROOM,
    CYBERPUNK_NEON,
    TOKYO_NIGHT,
    SOLARIZED_DARK,
    VINTAGE_BRITISH_LAB,
    TITANIUM_FROST;

    companion object {
        fun fromString(value: String?): ThemeMode = when (value) {
            "SYSTEM" -> SYSTEM
            "OBSIDIAN", "DARK" -> OBSIDIAN
            "AMBER_CRT" -> AMBER_CRT
            "HP_SLATE" -> HP_SLATE
            "CLEANROOM", "LIGHT" -> CLEANROOM
            "CYBERPUNK_NEON" -> CYBERPUNK_NEON
            "TOKYO_NIGHT" -> TOKYO_NIGHT
            "SOLARIZED_DARK" -> SOLARIZED_DARK
            "VINTAGE_BRITISH_LAB" -> VINTAGE_BRITISH_LAB
            "TITANIUM_FROST" -> TITANIUM_FROST
            else -> OBSIDIAN
        }
    }
}

/**
 * Every preference the app persists, as one immutable snapshot.
 */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.OBSIDIAN,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    /** LED afterglow for signals faster than the frame rate. */
    val ledAfterglow: Boolean = true,
    /** Bloom/glow halos on lit indicators and traces. */
    val bloomEffects: Boolean = true,
    /** Colour pre-armed for the next jumper wire drawn. */
    val defaultWireColor: String = "RED",
    /** `true` = right-angle routing for new wires, `false` = catenary. */
    val defaultRoutingManhattan: Boolean = false,
    /** Periodically write the open bench into the recovery draft. */
    val autosaveEnabled: Boolean = true
)

@Serializable
private data class SettingsDto(
    val themeMode: String = ThemeMode.OBSIDIAN.name,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val ledAfterglow: Boolean = true,
    val bloomEffects: Boolean = true,
    val defaultWireColor: String = "RED",
    val defaultRoutingManhattan: Boolean = false,
    val autosaveEnabled: Boolean = true
)

/**
 * Multiplatform preferences repository backed by [LogicLabsStorage].
 */
class SettingsRepository(
    private val storage: LogicLabsStorage = createDefaultStorage()
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadSync())
    val settings: Flow<Settings> = _settings.asStateFlow()

    val current: Settings get() = _settings.value

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setSoundEnabled(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

    fun setHapticsEnabled(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }

    fun setLedAfterglow(enabled: Boolean) = update { it.copy(ledAfterglow = enabled) }

    fun setBloomEffects(enabled: Boolean) = update { it.copy(bloomEffects = enabled) }

    fun setDefaultWireColor(colorName: String) = update { it.copy(defaultWireColor = colorName) }

    fun setDefaultRoutingManhattan(manhattan: Boolean) = update { it.copy(defaultRoutingManhattan = manhattan) }

    fun setAutosaveEnabled(enabled: Boolean) = update { it.copy(autosaveEnabled = enabled) }

    /** Resets every preference to factory defaults. */
    fun reset() = update { Settings() }

    private fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        scope.launch { persist(next) }
    }

    suspend fun flush(settings: Settings = _settings.value) {
        persist(settings)
    }

    private suspend fun persist(s: Settings) = mutex.withLock {
        runCatching {
            val dto = SettingsDto(
                themeMode = s.themeMode.name,
                soundEnabled = s.soundEnabled,
                hapticsEnabled = s.hapticsEnabled,
                ledAfterglow = s.ledAfterglow,
                bloomEffects = s.bloomEffects,
                defaultWireColor = s.defaultWireColor,
                defaultRoutingManhattan = s.defaultRoutingManhattan,
                autosaveEnabled = s.autosaveEnabled
            )
            storage.writeText(SETTINGS_PATH, json.encodeToString(SettingsDto.serializer(), dto))
        }
    }

    private fun loadSync(): Settings {
        val raw = storage.readText(SETTINGS_PATH) ?: return Settings()
        return runCatching {
            val dto = json.decodeFromString<SettingsDto>(raw)
            Settings(
                themeMode = ThemeMode.fromString(dto.themeMode),
                soundEnabled = dto.soundEnabled,
                hapticsEnabled = dto.hapticsEnabled,
                ledAfterglow = dto.ledAfterglow,
                bloomEffects = dto.bloomEffects,
                defaultWireColor = dto.defaultWireColor,
                defaultRoutingManhattan = dto.defaultRoutingManhattan,
                autosaveEnabled = dto.autosaveEnabled
            )
        }.getOrDefault(Settings())
    }

    private companion object {
        const val SETTINGS_PATH = "settings.json"
    }
}
