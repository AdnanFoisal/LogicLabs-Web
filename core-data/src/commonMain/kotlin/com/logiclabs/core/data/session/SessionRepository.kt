package com.logiclabs.core.data.session

import com.logiclabs.core.bridge.platform.currentTimeMillis
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

/** What kind of thing was on the bench when the user last left it. */
enum class SessionKind { LAB, PROJECT, SANDBOX }

/**
 * The pointer behind the Home screen's CONTINUE card.
 */
data class Session(
    val kind: SessionKind,
    val id: String?,
    val savedAtMillis: Long
)

@Serializable
private data class SessionDto(
    val kind: String,
    val id: String? = null,
    val savedAtMillis: Long = 0L
)

/**
 * Records what the user was last doing into `session.json` via [LogicLabsStorage].
 */
class SessionRepository(
    private val storage: LogicLabsStorage = createDefaultStorage()
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _lastSession = MutableStateFlow(loadSync())
    val lastSession: Flow<Session?> = _lastSession.asStateFlow()

    suspend fun current(): Session? = _lastSession.value

    fun recordLab(labId: String) = record(SessionKind.LAB, labId)

    fun recordProject(projectId: String) = record(SessionKind.PROJECT, projectId)

    fun recordSandbox() = record(SessionKind.SANDBOX, null)

    private fun record(kind: SessionKind, id: String?) {
        val next = Session(kind = kind, id = id, savedAtMillis = currentTimeMillis())
        _lastSession.value = next
        scope.launch {
            mutex.withLock {
                runCatching {
                    val dto = SessionDto(kind = next.kind.name, id = next.id, savedAtMillis = next.savedAtMillis)
                    storage.writeText(SESSION_PATH, json.encodeToString(SessionDto.serializer(), dto))
                }
            }
        }
    }

    private fun loadSync(): Session? {
        val raw = storage.readText(SESSION_PATH) ?: return null
        return runCatching {
            val dto = json.decodeFromString<SessionDto>(raw)
            val kind = SessionKind.valueOf(dto.kind)
            Session(kind = kind, id = dto.id, savedAtMillis = dto.savedAtMillis)
        }.getOrNull()
    }

    private companion object {
        const val SESSION_PATH = "session.json"
    }
}
