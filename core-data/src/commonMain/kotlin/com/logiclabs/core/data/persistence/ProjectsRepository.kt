package com.logiclabs.core.data.persistence

import com.logiclabs.core.bridge.platform.currentTimeMillis
import com.logiclabs.core.bridge.platform.ioDispatcher
import com.logiclabs.core.bridge.platform.randomUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** The stored form of a project plus whether this save created it. */
data class StoredProject(val project: SerializedProject, val isNew: Boolean)

/** A placed DIP, in the exact form a thumbnail can lay it back down. */
data class ChipSpot(
    val partNumber: String,
    val trench: Int,
    val startColumn: Int,
    val isRotated180: Boolean
)

/** One wire endpoint pair plus its insulation colour, for the same purpose. */
data class WireSpot(
    val startSocket: Int,
    val endSocket: Int,
    val colorArgb: Long
)

/** What a project list needs to show: identity, census, and enough geometry to sketch. */
data class ProjectSummary(
    val id: String,
    val title: String,
    val updatedAtMillis: Long,
    val chipCount: Int,
    val wireCount: Int,
    val activeLabId: String?,
    val chipSpots: List<ChipSpot> = emptyList(),
    val wireSpots: List<WireSpot> = emptyList()
)

/**
 * Multiplatform project store: one JSON document per project under `projects/<id>.json`.
 * Backed by atomic local files on Desktop (JVM) and `localStorage` on Web (WasmGC/JS).
 */
class ProjectsRepository(
    val storage: LogicLabsStorage = createDefaultStorage()
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    /** Bumped after every successful write/delete so UI can re-query [list]. */
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /** All projects, newest first. Corrupt files are skipped, never thrown. */
    suspend fun list(): List<ProjectSummary> = withContext(ioDispatcher) {
        val names = storage.listNames(PROJECTS_DIR).filter { it.endsWith(".json") && !it.startsWith("_") }
        names.mapNotNull { fileName ->
            val id = fileName.removeSuffix(".json")
            runCatching {
                val raw = storage.readText("$PROJECTS_DIR/$fileName") ?: return@mapNotNull null
                val p = json.decodeFromString<SerializedProject>(raw)
                id.toProjectSummary(p)
            }.getOrNull()
        }.sortedByDescending { it.updatedAtMillis }
    }

    suspend fun load(id: String): SerializedProject? = withContext(ioDispatcher) {
        val raw = storage.readText(pathFor(id)) ?: return@withContext null
        runCatching { json.decodeFromString<SerializedProject>(raw) }.getOrNull()
    }

    suspend fun save(project: SerializedProject): StoredProject = mutex.withLock {
        withContext(ioDispatcher) {
            val isNew = project.id.isBlank()
            val settled = if (isNew) {
                project.copy(id = randomUuid())
            } else {
                project
            }.copy(timestampMillis = currentTimeMillis())
            storage.writeText(
                pathFor(settled.id),
                json.encodeToString(SerializedProject.serializer(), settled)
            )
            _version.value++
            StoredProject(settled, isNew)
        }
    }

    suspend fun delete(id: String): Boolean = mutex.withLock {
        withContext(ioDispatcher) {
            val gone = storage.delete(pathFor(id))
            if (gone) _version.value++
            gone
        }
    }

    suspend fun rename(id: String, title: String) {
        val p = load(id) ?: return
        if (title.isBlank() || title == p.title) return
        save(p.copy(title = title.trim()))
    }

    /** Raw JSON text for sharing; `null` when the project does not exist. */
    suspend fun exportText(id: String): String? = withContext(ioDispatcher) {
        storage.readText(pathFor(id))
    }

    /** Parses shared JSON text and stores it as a new project. */
    suspend fun import(text: String): SerializedProject? {
        val parsed = runCatching { json.decodeFromString<SerializedProject>(text) }.getOrNull()
            ?: return null
        val stored = parsed.copy(id = randomUuid())
        save(stored)
        return stored
    }

    // -- Draft (autosave / crash recovery) -----------------------------------

    suspend fun saveDraft(project: SerializedProject) {
        mutex.withLock {
            withContext(ioDispatcher) {
                storage.writeText(
                    DRAFT_PATH,
                    json.encodeToString(SerializedProject.serializer(), project.copy(id = DRAFT_ID))
                )
            }
        }
    }

    suspend fun loadDraft(): SerializedProject? = withContext(ioDispatcher) {
        val raw = storage.readText(DRAFT_PATH) ?: return@withContext null
        runCatching { json.decodeFromString<SerializedProject>(raw) }.getOrNull()
    }

    suspend fun clearDraft() {
        mutex.withLock {
            withContext(ioDispatcher) { storage.delete(DRAFT_PATH) }
        }
    }

    fun saveDraftAsync(project: SerializedProject) {
        scope.launch { saveDraft(project) }
    }

    private fun pathFor(id: String): String {
        val safe = id.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { randomUuid() }
        return "$PROJECTS_DIR/$safe.json"
    }

    private fun String.toProjectSummary(p: SerializedProject) = ProjectSummary(
        id = this,
        title = p.title,
        updatedAtMillis = p.timestampMillis,
        chipCount = p.chips.size,
        wireCount = p.wires.size,
        activeLabId = p.activeLabId,
        chipSpots = p.chips.map { ChipSpot(it.partNumber, it.trench, it.startColumn, it.isRotated180) },
        wireSpots = p.wires.map { WireSpot(it.startSocket, it.endSocket, wireArgbOf(it.colorName)) }
    )

    private fun wireArgbOf(colorName: String): Long = try {
        com.logiclabs.core.bridge.model.WireColor.valueOf(colorName).hexArgb
    } catch (_: Exception) {
        com.logiclabs.core.bridge.model.WireColor.RED.hexArgb
    }

    private companion object {
        const val PROJECTS_DIR = "projects"
        const val DRAFT_ID = "_draft"
        const val DRAFT_PATH = "projects/_draft.json"
    }
}
