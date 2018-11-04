package com.soywiz.klock

import com.soywiz.klock.internal.*
import kotlin.math.*

/**
 * [TimeSpan] representing this number as [nanoseconds] or 1 / 1_000_000_000 [seconds].
 */
inline val Number.nanoseconds get() = TimeSpan.fromNanoseconds(this.toDouble())

/**
 * [TimeSpan] representing this number as [microseconds] or 1 / 1_000_000 [seconds].
 */
inline val Number.microseconds get() = TimeSpan.fromMicroseconds(this.toDouble())

/**
 * [TimeSpan] representing this number as [milliseconds] or 1 / 1_000 [seconds].
 */
inline val Number.milliseconds get() = TimeSpan.fromMilliseconds(this.toDouble())

/**
 * [TimeSpan] representing this number as [seconds].
 */
inline val Number.seconds get() = TimeSpan.fromSeconds((this.toDouble()))

/**
 * [TimeSpan] representing this number as [minutes] or 60 [seconds].
 */
inline val Number.minutes get() = TimeSpan.fromMinutes(this.toDouble())

/**
 * [TimeSpan] representing this number as [hours] or 3_600 [seconds].
 */
inline val Number.hours get() = TimeSpan.fromHours(this.toDouble())

/**
 * [TimeSpan] representing this number as [days] or 86_400 [seconds].
 */
inline val Number.days get() = TimeSpan.fromDays(this.toDouble())

/**
 * [TimeSpan] representing this number as [weeks] or 604_800 [seconds].
 */
inline val Number.weeks get() = TimeSpan.fromWeeks(this.toDouble())

/**
 * Represents a span of time, with [milliseconds] precision.
 *
 * It is an inline class wrapping [Double] instead of [Long] to work on JavaScript without allocations.
 */
@Suppress("DataClassPrivateConstructor")
inline class TimeSpan(val milliseconds: Double) : Comparable<TimeSpan> {
    val nanoseconds: Double get() = this.milliseconds * 1_000_000.0
    val microseconds: Double get() = this.milliseconds * 1_000.0
    val millisecondsLong: Long get() = this.milliseconds.toLong()
    val millisecondsInt: Int get() = this.milliseconds.toInt()
    val seconds: Double get() = this.milliseconds / 1000.0
    val minutes: Double get() = this.milliseconds / 60_000.0
    val hours: Double get() = this.milliseconds / 3600_000.0
    val days: Double get() = this.milliseconds / (24 * 3600_000.0)

    companion object {
        private const val MILLIS_PER_MICROSECOND = 1.0 / 1000.0
        private const val MILLIS_PER_NANOSECOND = MILLIS_PER_MICROSECOND / 1000.0

        /**
         * Zero time
         */
        val ZERO = TimeSpan(0.0)

        /**
         * Represents an invalid TimeSpan.
         * Useful to represent an alternative "null" time lapse
         * avoiding the boxing of a nullable type.
         */
        val NULL = TimeSpan(Double.NaN)

        @PublishedApi
        internal fun fromMilliseconds(ms: Double) = when (ms) {
            0.0 -> ZERO
            else -> TimeSpan(ms)
        }

        @PublishedApi internal fun fromNanoseconds(s: Double) = fromMilliseconds(s * MILLIS_PER_NANOSECOND)
        @PublishedApi internal fun fromMicroseconds(s: Double) = fromMilliseconds(s * MILLIS_PER_MICROSECOND)
        @PublishedApi internal fun fromSeconds(s: Double) = fromMilliseconds(s * MILLIS_PER_SECOND)
        @PublishedApi internal fun fromMinutes(s: Double) = fromMilliseconds(s * MILLIS_PER_MINUTE)
        @PublishedApi internal fun fromHours(s: Double) = fromMilliseconds(s * MILLIS_PER_HOUR)
        @PublishedApi internal fun fromDays(s: Double) = fromMilliseconds(s * MILLIS_PER_DAY)
        @PublishedApi internal fun fromWeeks(s: Double) = fromMilliseconds(s * MILLIS_PER_WEEK)

        private val timeSteps = listOf(60, 60, 24)
        private fun toTimeStringRaw(totalMilliseconds: Double, components: Int = 3): String {
            var timeUnit = floor(totalMilliseconds / 1000.0).toInt()

            val out = arrayListOf<String>()

            for (n in 0 until components) {
                if (n == components - 1) {
                    out += timeUnit.padded(2)
                    break
                }
                val step = timeSteps.getOrNull(n) ?: throw RuntimeException("Just supported ${timeSteps.size} steps")
                val cunit = timeUnit % step
                timeUnit /= step
                out += cunit.padded(2)
            }

            return out.reversed().joinToString(":")
        }

        inline fun toTimeString(totalMilliseconds: Number, components: Int = 3, addMilliseconds: Boolean = false): String =
            toTimeString(totalMilliseconds.toDouble(), components, addMilliseconds)

        @PublishedApi
        internal fun toTimeString(totalMilliseconds: Double, components: Int = 3, addMilliseconds: Boolean = false): String {
            val milliseconds = (totalMilliseconds % 1000).toInt()
            val out = toTimeStringRaw(totalMilliseconds, components)
            return if (addMilliseconds) "$out.$milliseconds" else out
        }
    }

    override fun compareTo(other: TimeSpan): Int = this.milliseconds.compareTo(other.milliseconds)

    operator fun unaryMinus() = TimeSpan(-this.milliseconds)
    operator fun unaryPlus() = TimeSpan(+this.milliseconds)

    operator fun plus(other: TimeSpan): TimeSpan = TimeSpan(this.milliseconds + other.milliseconds)
    operator fun plus(other: MonthSpan): DateTimeSpan = DateTimeSpan(other, this)
    operator fun plus(other: DateTimeSpan): DateTimeSpan = DateTimeSpan(other.dateSpan, other.timeSpan + this)

    operator fun minus(other: TimeSpan): TimeSpan = this + (-other)
    operator fun minus(other: MonthSpan): DateTimeSpan = this + (-other)
    operator fun minus(other: DateTimeSpan): DateTimeSpan = this + (-other)

    operator fun times(scale: Int): TimeSpan = TimeSpan(this.milliseconds * scale)
    operator fun times(scale: Double): TimeSpan = TimeSpan((this.milliseconds * scale))
}

fun TimeSpan.toTimeString(components: Int = 3, addMilliseconds: Boolean = false): String =
    TimeSpan.toTimeString(milliseconds, components, addMilliseconds)
