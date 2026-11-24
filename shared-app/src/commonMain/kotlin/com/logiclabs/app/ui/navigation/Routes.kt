package com.logiclabs.app.ui.navigation

/**
 * What the bench should load when it opens.
 */
sealed interface BenchRequest {
    /** Rebuild whatever the last session was; falls back to the intro lab. */
    data object Continue : BenchRequest

    /** Load a curriculum experiment by id. */
    data class Lab(val labId: String) : BenchRequest

    /** Load a saved project by id. */
    data class Project(val projectId: String) : BenchRequest

    /** Load a shared `LOGIC-...` circuit bytecode directly onto the live breadboard (e.g. from a Web URL). */
    data class ImportCode(val code: String) : BenchRequest

    /** An empty powered bench. */
    data object Sandbox : BenchRequest

    companion object {
        fun from(raw: String?, id: String?): BenchRequest = when (raw) {
            "lab" -> id?.let { Lab(it) } ?: Continue
            "project" -> id?.let { Project(it) } ?: Continue
            "code", "circuit" -> id?.let { ImportCode(it) } ?: Continue
            "sandbox" -> Sandbox
            else -> Continue
        }
    }
}

/** Route names and the argument-encoded bench route. */
object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SETTINGS = "settings"

    private const val BENCH_PATTERN = "bench?request={request}&id={id}"

    const val ARG_REQUEST = "request"
    const val ARG_ID = "id"

    fun bench(request: BenchRequest): String = when (request) {
        BenchRequest.Continue -> "bench?request=continue"
        BenchRequest.Sandbox -> "bench?request=sandbox"
        is BenchRequest.Lab -> "bench?request=lab&id=${request.labId}"
        is BenchRequest.Project -> "bench?request=project&id=${request.projectId}"
        is BenchRequest.ImportCode -> "bench?request=circuit&id=${request.code}"
    }.let { path ->
        check(path.startsWith("bench")) { "route drift: $path" }
        path
    }

    val benchRoute: String get() = BENCH_PATTERN
}
