
package com.wizlabs.wizdipcharts

import android.content.Context
import org.json.JSONArray

/** One tank's calibration chart. volumes[d] = chart volume (litres) at dip d cm; volumes[0] = 0. */
class Tank(
    val kl: Int,
    val diameterCm: Int,
    val lengthCm: Int,
    val p1Cm: Int,
    val p2Cm: Int,
    val deadwoodLitres: Int,
    val volumes: DoubleArray,
) {
    val maxDipCm: Int get() = volumes.size - 1
    val label: String get() = "$kl KL"
}

sealed interface DipResult {
    data class Ok(
        val chartVolume: Double,
        val deadwood: Double,
        val netVolume: Double,
    ) : DipResult

    data object Empty : DipResult
    data object Invalid : DipResult
    data class OutOfRange(val maxDipCm: Int) : DipResult
}

sealed interface DipForVolumeResult {
    data class Ok(val dipCm: Double, val chartVolume: Double) : DipForVolumeResult
    data object Empty : DipForVolumeResult
    data object Invalid : DipForVolumeResult
    data class OutOfRange(val maxVolumeLitres: Double) : DipForVolumeResult
}

object Calibration {

    /**
     * Linear interpolation between the two whole-cm chart rows around [dip].
     * Dip 10.4 -> V(10) + 0.4 * (V(11) - V(10)).
     */
    fun chartVolume(tank: Tank, dip: Double): Double {
        val max = tank.maxDipCm
        if (dip <= 0.0) return 0.0
        if (dip >= max) return tank.volumes[max]

        val lo = dip.toInt()
        val frac = dip - lo
        val v0 = tank.volumes[lo]
        val v1 = tank.volumes[lo + 1]

        return v0 + frac * (v1 - v0)
    }

    /**
     * Dip to volume.
     * Uses the calibration chart value directly.
     * No additional deadwood subtraction is performed.
     */
    fun compute(tank: Tank, input: String): DipResult {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return DipResult.Empty

        val dip = text.toDoubleOrNull() ?: return DipResult.Invalid

        if (!dip.isFinite() || dip < 0.0) return DipResult.Invalid
        if (dip > tank.maxDipCm) return DipResult.OutOfRange(tank.maxDipCm)

        val chart = chartVolume(tank, dip)

        return DipResult.Ok(
            chartVolume = chart,
            deadwood = 0.0,
            netVolume = chart
        )
    }

    /**
     * Volume to dip.
     * Uses the entered volume directly against the calibration chart.
     * No deadwood is added to the entered volume.
     */
    fun dipForVolume(tank: Tank, input: String): DipForVolumeResult {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return DipForVolumeResult.Empty

        val target = text.toDoubleOrNull() ?: return DipForVolumeResult.Invalid

        if (!target.isFinite() || target < 0.0) {
            return DipForVolumeResult.Invalid
        }

        val v = tank.volumes
        val max = tank.maxDipCm

        if (target > v[max] + 1e-9) {
            return DipForVolumeResult.OutOfRange(v[max])
        }

        if (target >= v[max]) {
            return DipForVolumeResult.Ok(max.toDouble(), v[max])
        }

        // Find the largest whole-cm dip where chart volume <= target.
        var lo = 0
        var hi = max - 1

        while (lo < hi) {
            val mid = (lo + hi + 1) / 2

            if (v[mid] <= target) {
                lo = mid
            } else {
                hi = mid - 1
            }
        }

        val lowerVolume = v[lo]
        val upperVolume = v[lo + 1]
        val fraction = (target - lowerVolume) / (upperVolume - lowerVolume)
        val dip = lo + fraction

        return DipForVolumeResult.Ok(
            dipCm = dip,
            chartVolume = target
        )
    }

    /**
     * Loads and validates the offline calibration charts.
     */
    fun load(context: Context): List<Tank> {
        val json = context.assets.open("charts.json")
            .bufferedReader()
            .use { it.readText() }

        val arr = JSONArray(json)

        require(arr.length() > 0) {
            "No tank calibration charts found."
        }

        val loaded = List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val v = o.getJSONArray("volumes")
            val values = DoubleArray(v.length()) { v.getDouble(it) }

            val tank = Tank(
                kl = o.getInt("kl"),
                diameterCm = o.getInt("diameterCm"),
                lengthCm = o.getInt("lengthCm"),
                p1Cm = o.getInt("p1Cm"),
                p2Cm = o.getInt("p2Cm"),
                deadwoodLitres = o.getInt("deadwoodLitres"),
                volumes = values
            )

            require(
                tank.kl > 0 &&
                tank.diameterCm > 0 &&
                tank.lengthCm > 0
            ) {
                "Invalid tank dimensions for ${tank.kl} KL."
            }

            require(
                values.size >= 2 &&
                o.optInt("maxDipCm", values.size - 1) == values.size - 1
            ) {
                "Invalid dip range for ${tank.kl} KL."
            }

            require(kotlin.math.abs(values.first()) < 1e-9) {
                "Chart for ${tank.kl} KL must start at zero litres."
            }

            require(values.all { it.isFinite() && it >= 0.0 }) {
                "Invalid chart volume in ${tank.kl} KL."
            }

            require(
                (1 until values.size).all { index ->
                    values[index] > values[index - 1]
                }
            ) {
                "Chart for ${tank.kl} KL must be strictly increasing."
            }

            require(
                tank.deadwoodLitres >= 0 &&
                tank.deadwoodLitres < values.last()
            ) {
                "Invalid deadwood for ${tank.kl} KL."
            }

            tank
        }

        require(loaded.map { it.kl }.distinct().size == loaded.size) {
            "Duplicate tank sizes in calibration charts."
        }

        return loaded
    }
}
