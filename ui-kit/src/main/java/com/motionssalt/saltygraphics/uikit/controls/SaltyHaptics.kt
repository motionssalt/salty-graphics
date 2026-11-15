package com.motionssalt.saltygraphics.uikit.controls

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class ScrollHapticsMode(val label: String) {
    OFF("Off"),
    LIGHT("Light"),
    NORMAL("Normal")
}

val LocalScrollHapticsMode = compositionLocalOf { ScrollHapticsMode.LIGHT }

/**
 * Targeted & moderated haptics engine (V5 & V29).
 * Uses View.performHapticFeedback (no VIBRATE permission required) and respects
 * the Android system touch-feedback setting.
 */
object SaltyHaptics {
    @Volatile
    var performer: (View, Int) -> Unit = { view, constant ->
        view.performHapticFeedback(constant)
    }

    @Volatile
    var systemHapticsEnabledOverride: Boolean? = null

    fun isSystemTouchFeedbackEnabled(context: Context?): Boolean {
        systemHapticsEnabledOverride?.let { return it }
        if (context == null) return true
        return runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
        }.getOrDefault(true)
    }

    fun lightestTickConstant(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }

    fun snapTickConstant(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }

    fun guideLockConstant(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }

    fun onScrollTick(view: View?) {
        if (view == null) return
        if (!isSystemTouchFeedbackEnabled(view.context)) return
        performer(view, lightestTickConstant())
    }

    fun onSnapEngaged(view: View?) {
        if (view == null) return
        if (!isSystemTouchFeedbackEnabled(view.context)) return
        performer(view, snapTickConstant())
    }

    fun onGuideLocked(view: View?) {
        if (view == null) return
        if (!isSystemTouchFeedbackEnabled(view.context)) return
        performer(view, guideLockConstant())
    }

    fun onKeyToggle(view: View?) {
        if (view == null) return
        if (!isSystemTouchFeedbackEnabled(view.context)) return
        performer(view, HapticFeedbackConstants.CLOCK_TICK)
    }
}

/**
 * Pure, testable rate-limiter and major-tick filter for scroll-wheel / ruler haptics (V5).
 */
class RulerHapticThrottle(
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private var lastFireMs: Long = Long.MIN_VALUE
    private val recentFires = ArrayDeque<Long>()

    fun reset() {
        lastFireMs = Long.MIN_VALUE
        recentFires.clear()
    }

    fun onDragValueChanged(
        mode: ScrollHapticsMode,
        prevValue: Float,
        newValue: Float,
        step: Float,
        defaultValue: Float,
        isFling: Boolean,
        systemEnabled: Boolean = true,
        onTick: () -> Unit
    ): Boolean {
        if (!shouldFire(mode, prevValue, newValue, step, defaultValue, isFling, systemEnabled)) {
            return false
        }
        onTick()
        return true
    }

    fun shouldFire(
        mode: ScrollHapticsMode,
        prevValue: Float,
        newValue: Float,
        step: Float,
        defaultValue: Float,
        isFling: Boolean,
        systemEnabled: Boolean = true
    ): Boolean {
        if (isFling || mode == ScrollHapticsMode.OFF || !systemEnabled) return false
        if (!prevValue.isFinite() || !newValue.isFinite() || prevValue == newValue) return false

        val safeStep = step.coerceAtLeast(0.01f)
        val majorEvery = if (mode == ScrollHapticsMode.LIGHT) 10 else 5
        val snapEps = safeStep * 0.08f

        val crossedZero = crossedPoint(prevValue, newValue, 0f, snapEps)
        val crossedDefault = crossedPoint(prevValue, newValue, defaultValue, snapEps)
        val crossedMajorTick = crossedMultiple(prevValue, newValue, safeStep * majorEvery)
        val crossedWholeUnit = mode == ScrollHapticsMode.NORMAL && safeStep < 1f &&
            crossedMultiple(prevValue, newValue, 1f)

        if (!crossedZero && !crossedDefault && !crossedMajorTick && !crossedWholeUnit) {
            return false
        }

        val now = nowMs()
        val minIntervalMs = if (mode == ScrollHapticsMode.LIGHT) 100L else 65L
        if (lastFireMs != Long.MIN_VALUE && (now - lastFireMs) < minIntervalMs) {
            return false
        }

        while (recentFires.isNotEmpty() && (now - recentFires.first()) >= 1000L) {
            recentFires.removeFirst()
        }
        val maxPerSecond = if (mode == ScrollHapticsMode.LIGHT) 5 else 8
        if (recentFires.size >= maxPerSecond) {
            return false
        }

        lastFireMs = now
        recentFires.addLast(now)
        return true
    }

    private fun crossedPoint(prev: Float, next: Float, target: Float, eps: Float): Boolean {
        if (abs(prev - target) <= eps) return false
        if (abs(next - target) <= eps) return true
        return (prev < target && next > target) || (prev > target && next < target)
    }

    private fun crossedMultiple(prev: Float, next: Float, spacing: Float): Boolean {
        if (spacing <= 0f) return false
        val lo = min(prev, next)
        val hi = max(prev, next)
        val firstK = ceil((lo / spacing).toDouble() + 1e-6).toLong()
        val lastK = floor((hi / spacing).toDouble() + 1e-6).toLong()
        if (firstK > lastK) return false
        // Ignore if prev was already sitting on the only matched tick
        if (firstK == lastK) {
            val tickVal = firstK * spacing
            if (abs(prev - tickVal) <= spacing * 1e-4f) return false
        }
        return true
    }
}
