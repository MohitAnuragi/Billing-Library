package com.billing.ads

// One step = "ask one network for an ad" and report the result
internal typealias LoadStep = (onLoaded: () -> Unit, onFailed: (String) -> Unit) -> Unit

// Asks the networks one after the other and stops at the first one that has an ad.
// Only one load runs at a time: extra calls join the load that is already running.
internal class WaterfallLoader(private val isReady: () -> Boolean) {

    private var isLoading = false
    private val loadedCallbacks = mutableListOf<() -> Unit>()
    private val failedCallbacks = mutableListOf<(String) -> Unit>()

    fun load(steps: List<LoadStep>, onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        if (isReady()) {
            onLoaded()
            return
        }
        if (steps.isEmpty()) {
            onFailed("No ad unit ID provided")
            return
        }

        loadedCallbacks.add(onLoaded)
        failedCallbacks.add(onFailed)
        if (isLoading) return

        isLoading = true
        runStep(steps, 0, "")
    }

    private fun runStep(steps: List<LoadStep>, index: Int, lastError: String) {
        if (index >= steps.size) {
            finishFailed(lastError)
            return
        }

        // A network may answer more than once; only its first answer counts
        var isAnswered = false
        steps[index](
            {
                if (!isAnswered) {
                    isAnswered = true
                    finishLoaded()
                }
            },
            { error ->
                if (!isAnswered) {
                    isAnswered = true
                    runStep(steps, index + 1, error)
                }
            }
        )
    }

    private fun finishLoaded() {
        val callbacks = loadedCallbacks.toList()
        reset()
        callbacks.forEach { it() }
    }

    private fun finishFailed(error: String) {
        val callbacks = failedCallbacks.toList()
        reset()
        callbacks.forEach { it(error) }
    }

    // Runs before the callbacks, so a callback may start a new load
    private fun reset() {
        loadedCallbacks.clear()
        failedCallbacks.clear()
        isLoading = false
    }
}
