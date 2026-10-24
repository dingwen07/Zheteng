package net.extrawdw.apps.zheteng.ui.main

import net.extrawdw.apps.zheteng.data.Challenge
import net.extrawdw.apps.zheteng.data.Screen

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

class AdViewModel(private val saved: SavedStateHandle) : ViewModel() {
    // Deadlines use accumulated visible ad time, never a persisted device uptime.
    private val restoredClock = saved.get<Int>("clockVersion") == 1
    private var activeMillis: Long = if (restoredClock) saved["activeMillis"] ?: 0L else 0L
    private var visibleMillis: Long = if (restoredClock) saved["visibleMillis"] ?: activeMillis else 0L
    private var foreground = false
    private var lastRealtime: Long? = null
    var state by mutableStateOf(if (restoredClock) Challenge(
        deadline = saved["deadline"] ?: (activeMillis + Challenge.DURATION),
        screen = enumValueOf(saved["screen"] ?: "AD"),
        finish = enumValueOf(saved["finish"] ?: "NONE"),
        angle = saved["angle"], ready = saved["ready"] ?: false,
        minimum = saved["minimum"], attempts = saved["attempts"] ?: 0,
        redirects = saved["redirects"] ?: 0,
        shop = enumValueOf(saved["shop"] ?: "TAOBAO"), lastMinimum = saved["lastMinimum"],
        completedAt = saved["completedAt"], foldStartedAt = saved["foldStartedAt"],
        redirectReason = enumValueOf(saved["redirectReason"] ?: "TOO_SHALLOW")
    ) else Challenge.start(0))
        private set
    var seconds by mutableIntStateOf(state.remaining(activeMillis))
        private set
    var demo by mutableStateOf(saved["demo"] ?: false)
        private set
    val openingSeconds: Int
        get() = ceil(visibleMillis / 1000.0).toInt()

    init {
        persist()
        viewModelScope.launch {
            while (true) {
                if (foreground && state.screen != Screen.HOME) advanceClock()
                delay(100)
            }
        }
    }

    fun setForeground(visible: Boolean) {
        if (foreground == visible) return
        if (foreground) advanceClock()
        foreground = visible
        lastRealtime = if (visible) SystemClock.elapsedRealtime() else null
        persist()
    }

    private fun advanceClock() {
        val now = SystemClock.elapsedRealtime()
        val previous = lastRealtime
        if (foreground && previous != null && state.screen != Screen.HOME) {
            val elapsed = (now - previous).coerceAtLeast(0)
            if (state.screen == Screen.AD && state.ready && state.angle != null) {
                val adElapsed = elapsed.coerceAtMost((state.deadline - activeMillis).coerceAtLeast(0))
                activeMillis += adElapsed
                visibleMillis += adElapsed
            } else if (state.screen == Screen.REDIRECT) {
                visibleMillis += elapsed
            }
        }
        lastRealtime = if (foreground) now else null
        update(state.tick(activeMillis))
        saved["activeMillis"] = activeMillis
        saved["visibleMillis"] = visibleMillis
    }

    fun sample(angle: Float) {
        if (!foreground) return
        advanceClock()
        update(state.sample(angle, activeMillis))
    }
    fun returnToAd() {
        advanceClock()
        update(state.returnToAd(activeMillis))
        if (demo) sample(180f)
    }
    fun enableDemo() {
        advanceClock()
        demo = true
        saved["demo"] = true
        if (state.screen == Screen.AD) {
            update(state.copy(angle = 180f, ready = true, minimum = null, foldStartedAt = null))
        }
    }
    fun restart() {
        activeMillis = 0
        visibleMillis = 0
        lastRealtime = if (foreground) SystemClock.elapsedRealtime() else null
        update(Challenge.start(0))
        if (demo) sample(180f)
    }
    private fun update(next: Challenge) {
        if (next != state) {
            state = next
            persist()
        }
        seconds = state.remaining(activeMillis)
    }
    private fun persist() {
        saved["clockVersion"] = 1
        saved["activeMillis"] = activeMillis
        saved["visibleMillis"] = visibleMillis
        saved["deadline"] = state.deadline
        saved["screen"] = state.screen.name
        saved["finish"] = state.finish.name
        saved["angle"] = state.angle
        saved["ready"] = state.ready
        saved["minimum"] = state.minimum
        saved["attempts"] = state.attempts
        saved["redirects"] = state.redirects
        saved["shop"] = state.shop.name
        saved["lastMinimum"] = state.lastMinimum
        saved["completedAt"] = state.completedAt
        saved["foldStartedAt"] = state.foldStartedAt
        saved["redirectReason"] = state.redirectReason.name
    }
}
