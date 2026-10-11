// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

import java.util.ArrayDeque

/**
 * Collects independent positive OCR readings over a short sliding time window.
 * Hand movement, glare and momentary focus loss may produce null frames; they
 * must not erase previous correct readings. Conflicting readings remain evidence
 * against acceptance. A value is NEVER stored without user confirmation.
 */
internal class ReadingStability(
    private val required: Int = 3,
    private val windowMs: Long = 9000L,
    private val minAgreement: Double = 0.75,
    private val minTimeSpanMs: Long = 550L
) {
    init {
        require(required >= 2)
        require(windowMs > minTimeSpanMs && minTimeSpanMs >= 0L)
        require(minAgreement > 0.5 && minAgreement <= 1.0)
    }

    private data class Reading(val value: Int, val timestampMs: Long)
    private val history = ArrayDeque<Reading>()
    private var lastTimestampMs: Long? = null
    var matches: Int = 0
        private set
    var candidate: Int? = null
        private set

    fun reset() {
        history.clear()
        lastTimestampMs = null
        matches = 0
        candidate = null
    }

    /** A true result is only returned for a new positive OCR observation. */
    fun offer(value: Int?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastTimestampMs != null && nowMs < lastTimestampMs!!) reset()
        lastTimestampMs = nowMs
        while (history.isNotEmpty() && nowMs - history.first().timestampMs > windowMs) {
            history.removeFirst()
        }
        // An absent or implausible reading is NOT a vote and cannot complete recognition.
        if (value != null && value in 20..600) {
            history.addLast(Reading(value, nowMs))
        }
        // Keep processing bounded, including a device that runs OCR much more quickly.
        while (history.size > 24) history.removeFirst()
        val groups = history.groupingBy { it.value }.eachCount()
        val top = groups.entries.sortedWith(
            compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key }
        ).firstOrNull()
        candidate = top?.key
        matches = (top?.value ?: 0).coerceAtMost(required)
        if (value == null || top == null || value != top.key || top.value < required) return false
        val agreement = top.value.toDouble() / history.size
        if (agreement < minAgreement) return false
        val earliest = history.firstOrNull { it.value == value }?.timestampMs ?: return false
        return nowMs - earliest >= minTimeSpanMs
    }
}
