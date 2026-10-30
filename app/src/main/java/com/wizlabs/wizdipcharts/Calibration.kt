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

    /** The calibration chart value is used directly; deadwood is display metadata only. */
    fun compute(tank: Tank, input: String): DipResult {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return DipResult.Empty
        val dip = text.toDoubleOrNull() ?: return DipResult.Invalid
        if (dip.isNaN() || dip < 0.0) return DipResult.Invalid
        if (dip > tank.maxDipCm) return DipResult.OutOfRange(tank.maxDipCm)
        val chart = chartVolume(tank, dip)
        return DipResult.Ok(chart, 0.0, chart)
    }

    /**
     * Reverse lookup: find the dip (cm) for the entered chart volume (litres).
     * No deadwood adjustment is applied.
     */
    fun dipForVolume(tank: Tank, input: String): DipForVolumeResult {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return DipForVolumeResult.Empty
        val target = text.toDoubleOrNull() ?: return DipForVolumeResult.Invalid
        if (target.isNaN() || target < 0.0) return DipForVolumeResult.Invalid

        val v = tank.volumes
        val max = tank.maxDipCm
        if (target > v[max] + 1e-9) {
            return DipForVolumeResult.OutOfRange(v[max])
        }
        if (target >= v[max]) return DipForVolumeResult.Ok(max.toDouble(), v[max])

        // largest lo in [0, max-1] with v[lo] <= target (v is strictly increasing)
        var lo = 0
        var hi = max - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (v[mid] <= target) lo = mid else hi = mid - 1
        }
        val dip = lo + (target - v[lo]) / (v[lo + 1] - v[lo])
        return DipForVolumeResult.Ok(dip, target)
    }

    fun load(context: Context): List<Tank> {
        val json = context.assets.open("charts.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        require(arr.length() > 0) { "No tank calibration charts found." }
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
                volumes = values,
            )
            require(tank.kl > 0 && tank.diameterCm > 0 && tank.lengthCm > 0) { "Invalid tank dimensions for ${tank.kl} KL." }
            require(values.size >= 2 && o.optInt("maxDipCm", values.size - 1) == values.size - 1) { "Invalid dip range for ${tank.kl} KL." }
            require(kotlin.math.abs(values.first()) < 1e-9) { "Chart for ${tank.kl} KL must start at zero litres." }
            require(values.all { it.isFinite() && it >= 0.0 }) { "Invalid chart volume in ${tank.kl} KL." }
            require((1 until values.size).all { index -> values[index] > values[index - 1] }) { "Chart for ${tank.kl} KL must be strictly increasing." }
            require(tank.deadwoodLitres >= 0 && tank.deadwoodLitres < values.last()) { "Invalid deadwood for ${tank.kl} KL." }
            tank
        }
        require(loaded.map { it.kl }.distinct().size == loaded.size) { "Duplicate tank sizes in calibration charts." }
        return loaded
    }
}
