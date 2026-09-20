package com.davidhuynh.levelup.domain.logic

import com.davidhuynh.levelup.domain.model.WeightUnit
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Weight is stored in kilograms everywhere in the database. This converts at the edges,
 * so switching the display unit never rewrites stored data.
 */
object WeightConverter {

    const val KG_PER_LB = 0.45359237

    fun toKg(value: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> value
        WeightUnit.LB -> value * KG_PER_LB
    }

    fun fromKg(valueKg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> valueKg
        WeightUnit.LB -> valueKg / KG_PER_LB
    }

    /** Weight for an input field: one decimal, and no trailing ".0" to retype around. */
    fun formatValue(valueKg: Double, unit: WeightUnit): String =
        trimNumber(fromKg(valueKg, unit), decimals = 1)

    /** Weight with its unit, for display. */
    fun format(valueKg: Double, unit: WeightUnit): String =
        "${formatValue(valueKg, unit)} ${unit.suffix}"

    /**
     * Total volume, which reaches six figures fast, so it is abbreviated: 12.4k lb.
     */
    fun formatVolume(volumeKg: Double, unit: WeightUnit): String {
        val value = fromKg(volumeKg, unit)
        val abs = abs(value)
        return when {
            abs >= 1_000_000 -> "${trimNumber(value / 1_000_000, 1)}M ${unit.suffix}"
            abs >= 10_000 -> "${trimNumber(value / 1_000, 1)}k ${unit.suffix}"
            else -> "${value.roundToLong()} ${unit.suffix}"
        }
    }

    private fun trimNumber(value: Double, decimals: Int): String {
        val factor = when (decimals) {
            0 -> 1.0
            1 -> 10.0
            else -> 100.0
        }
        val rounded = (value * factor).roundToLong() / factor
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
