package com.logiclabs.core.data.progress

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

/**
 * Lifetime bench statistics — verified labs, circuits built, bench time, and badges.
 */
data class BenchStats(
    val verifiedLabIds: Set<String> = emptySet(),
    val circuitsBuilt: Int = 0,
    val benchTimeMillis: Long = 0L,
    val firstLaunchAtMillis: Long = 0L
) {
    val benchTimeMinutes: Long get() = benchTimeMillis / 60_000
}

/** A single named achievement. Earned state is derived, never stored. */
data class Badge(
    val id: String,
    val title: String,
    val description: String
)

/** The badge catalog and the predicates that light each one. */
object BadgeCatalog {

    val FIRST_CIRCUIT = Badge("first_circuit", "First Circuit", "Built your first circuit")
    val TRUTH_SEEKER = Badge("truth_seeker", "Truth Seeker", "Verified your first experiment")
    val GATE_KEEPER = Badge("gate_keeper", "Gate Keeper", "Verified four experiments")
    val HALF_COURSE = Badge("half_course", "Halfway Bench", "Verified half the classic course")
    val FULL_COURSE = Badge("full_course", "Full Course", "Verified every classic lab")
    val BENCH_TIME = Badge("bench_time", "Bench Time", "An hour on the bench")

    val all: List<Badge> = listOf(
        FIRST_CIRCUIT,
        TRUTH_SEEKER,
        GATE_KEEPER,
        HALF_COURSE,
        FULL_COURSE,
        BENCH_TIME
    )

    fun earnedBy(stats: BenchStats, classicLabCount: Int): Set<String> = buildSet {
        if (stats.circuitsBuilt >= 1) add(FIRST_CIRCUIT.id)
        if (stats.verifiedLabIds.isNotEmpty()) add(TRUTH_SEEKER.id)
        if (stats.verifiedLabIds.size >= 4) add(GATE_KEEPER.id)
        if (classicLabCount > 0 && stats.verifiedLabIds.size >= (classicLabCount + 1) / 2) add(HALF_COURSE.id)
        if (classicLabCount > 0 && stats.verifiedLabIds.size >= classicLabCount) add(FULL_COURSE.id)
        if (stats.benchTimeMillis >= 60L * 60_000) add(BENCH_TIME.id)
    }

    fun derive(stats: BenchStats, classicLabCount: Int): List<Pair<Badge, Boolean>> =
        all.map { it to (it.id in earnedBy(stats, classicLabCount)) }

    fun newlyEarned(before: BenchStats, after: BenchStats, classicLabCount: Int): List<Badge> {
        if (before == after) return emptyList()
        val b = earnedBy(before, classicLabCount)
        return all.filter { it.id !in b && it.id in earnedBy(after, classicLabCount) }
    }
}

@Serializable
private data class ProgressDto(
    val verifiedLabIds: Set<String> = emptySet(),
    val circuitsBuilt: Int = 0,
    val benchTimeMillis: Long = 0L,
    val firstLaunchAtMillis: Long = 0L
)

/**
 * Persists [BenchStats] via [LogicLabsStorage] (`progress.json`).
 */
class ProgressRepository(
    private val storage: LogicLabsStorage = createDefaultStorage()
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _stats = MutableStateFlow(loadSync())
    val stats: Flow<BenchStats> = _stats.asStateFlow()

    fun ensureFirstLaunch() {
        scope.launch {
            mutex.withLock {
                val cur = _stats.value
                if (cur.firstLaunchAtMillis == 0L) {
                    val next = cur.copy(firstLaunchAtMillis = currentTimeMillis())
                    _stats.value = next
                    persistLocked(next)
                }
            }
        }
    }

    suspend fun markLabVerified(labId: String, classicLabCount: Int): List<Badge> = mutex.withLock {
        val before = _stats.value
        val after = before.copy(verifiedLabIds = before.verifiedLabIds + labId)
        _stats.value = after
        persistLocked(after)
        BadgeCatalog.newlyEarned(before, after, classicLabCount)
    }

    suspend fun incrementCircuitsBuilt(): List<Badge> = mutex.withLock {
        val before = _stats.value
        val after = before.copy(circuitsBuilt = before.circuitsBuilt + 1)
        _stats.value = after
        persistLocked(after)
        BadgeCatalog.newlyEarned(before, after, 0)
    }

    fun addBenchTime(millis: Long) {
        if (millis <= 0) return
        scope.launch {
            mutex.withLock {
                val next = _stats.value.copy(benchTimeMillis = _stats.value.benchTimeMillis + millis)
                _stats.value = next
                persistLocked(next)
            }
        }
    }

    fun reset() {
        scope.launch {
            mutex.withLock {
                val next = BenchStats()
                _stats.value = next
                persistLocked(next)
            }
        }
    }

    private fun persistLocked(s: BenchStats) {
        runCatching {
            val dto = ProgressDto(
                verifiedLabIds = s.verifiedLabIds,
                circuitsBuilt = s.circuitsBuilt,
                benchTimeMillis = s.benchTimeMillis,
                firstLaunchAtMillis = s.firstLaunchAtMillis
            )
            storage.writeText(PROGRESS_PATH, json.encodeToString(ProgressDto.serializer(), dto))
        }
    }

    private fun loadSync(): BenchStats {
        val raw = storage.readText(PROGRESS_PATH) ?: return BenchStats()
        return runCatching {
            val dto = json.decodeFromString<ProgressDto>(raw)
            BenchStats(
                verifiedLabIds = dto.verifiedLabIds,
                circuitsBuilt = dto.circuitsBuilt,
                benchTimeMillis = dto.benchTimeMillis,
                firstLaunchAtMillis = dto.firstLaunchAtMillis
            )
        }.getOrDefault(BenchStats())
    }

    private companion object {
        const val PROGRESS_PATH = "progress.json"
    }
}
