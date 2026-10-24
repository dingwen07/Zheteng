package net.extrawdw.apps.zheteng.data

import net.extrawdw.apps.zheteng.R

import kotlin.math.ceil
import androidx.annotation.StringRes

enum class Screen { AD, REDIRECT, HOME }
enum class Finish { NONE, FOLDED, WAITED }
enum class RedirectReason { TOO_SHALLOW, TOO_DEEP, TOO_SLOW }
enum class Shop(@param:StringRes val titleRes: Int, @param:StringRes val taglineRes: Int, val color: Long) {
    TAOBAO(R.string.shop_xbao, R.string.tagline_xbao, 0xFFFF6200),
    JD(R.string.shop_xdong, R.string.tagline_xdong, 0xFFE52629),
    PDD(R.string.shop_xxixi, R.string.tagline_xxixi, 0xFFE52C32)
}

/** Pure, clock-driven state machine. Fold angle: 0 = closed, 180 = flat. */
data class Challenge(
    val deadline: Long,
    val screen: Screen = Screen.AD,
    val finish: Finish = Finish.NONE,
    val angle: Float? = null,
    val ready: Boolean = false,
    val minimum: Float? = null,
    val attempts: Int = 0,
    val redirects: Int = 0,
    val shop: Shop = Shop.TAOBAO,
    val lastMinimum: Float? = null,
    val completedAt: Long? = null,
    val foldStartedAt: Long? = null,
    val redirectReason: RedirectReason = RedirectReason.TOO_SHALLOW
) {
    fun remaining(now: Long): Int = ceil((deadline - (completedAt ?: now)).coerceAtLeast(0) / 1000.0).toInt()

    fun gestureRemaining(now: Long): Long =
        (GESTURE_LIMIT - (now - (foldStartedAt ?: now))).coerceIn(0L, GESTURE_LIMIT)

    fun tick(now: Long, chooseShop: () -> Shop = { Shop.entries.random() }): Challenge = when {
        screen == Screen.AD && now >= deadline ->
            copy(screen = Screen.HOME, finish = Finish.WAITED, completedAt = deadline, foldStartedAt = null)
        screen == Screen.AD && foldStartedAt != null && gestureRemaining(now) == 0L ->
            copy(screen = Screen.REDIRECT, deadline = now + DURATION, attempts = attempts + 1, redirects = redirects + 1,
                shop = chooseShop(), lastMinimum = minimum, minimum = null,
                foldStartedAt = null, redirectReason = RedirectReason.TOO_SLOW)
        else -> this
    }

    fun sample(value: Float, now: Long, chooseShop: () -> Shop = { Shop.entries.random() }): Challenge {
        val current = tick(now, chooseShop)
        if (!value.isFinite() || value !in 0f..180f || current.screen == Screen.HOME) return current
        val next = current.copy(angle = value)
        if (current.screen == Screen.REDIRECT) return next
        // Require a flat starting position. Hysteresis prevents sensor noise from creating attempts.
        if (!ready) return next.copy(ready = value >= FLAT)
        if (minimum == null) return if (value < START) next.copy(minimum = value, foldStartedAt = now) else next
        val low = minOf(minimum, value)
        if (value < FLAT) return next.copy(minimum = low)
        return if (low in TARGET_MIN..TARGET_MAX) {
            next.copy(screen = Screen.HOME, finish = Finish.FOLDED, attempts = attempts + 1,
                lastMinimum = low, minimum = null, completedAt = now, foldStartedAt = null)
        } else {
            next.copy(screen = Screen.REDIRECT, deadline = now + DURATION, attempts = attempts + 1, redirects = redirects + 1,
                shop = chooseShop(), lastMinimum = low, minimum = null, foldStartedAt = null,
                redirectReason = if (low < TARGET_MIN) RedirectReason.TOO_DEEP else RedirectReason.TOO_SHALLOW)
        }
    }

    fun returnToAd(now: Long): Challenge = tick(now).let {
        if (it.screen == Screen.REDIRECT) it.copy(screen = Screen.AD, ready = (it.angle ?: 0f) >= FLAT) else it
    }

    companion object {
        const val DURATION = 120_000L
        const val GESTURE_LIMIT = 3_000L
        const val FLAT = 165f
        const val START = 165f
        const val TARGET_MIN = 100f
        const val TARGET_MAX = 120f
        fun start(now: Long) = Challenge(deadline = now + DURATION)
    }
}
