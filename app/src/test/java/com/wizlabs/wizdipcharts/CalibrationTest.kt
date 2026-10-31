package com.wizlabs.wizdipcharts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {
    // 20 KL chart rows: dip 10 = 375.511, dip 11 = 432.628; deadwood 18 L
    // Strictly increasing synthetic table that hits the two real chart rows around dip 10.
    private val volumes = DoubleArray(211) { i ->
        if (i <= 10) 37.551090903757921 * i
        else 375.51090903757921 + (i - 10) * (432.62814996693123 - 375.51090903757921)
    }
    private val tank = Tank("test-20", "20 KL test chart", 20, 210.0, 625.0, 0, 0, 18, volumes)

    @Test fun interpolatesBetweenWholeDips() {
        val r = Calibration.compute(tank, "10.4") as DipResult.Ok
        assertEquals(398.358, r.chartVolume, 0.001)
        assertEquals(398.358, r.netVolume, 0.001)
    }

    @Test fun wholeDipUsesChartValueDirectly() {
        val r = Calibration.compute(tank, "10") as DipResult.Ok
        assertEquals(375.511, r.chartVolume, 0.001)
    }

    @Test fun commaDecimalAccepted() {
        assertTrue(Calibration.compute(tank, "10,4") is DipResult.Ok)
    }

    @Test fun aboveMaxIsOutOfRange() {
        assertTrue(Calibration.compute(tank, "210.5") is DipResult.OutOfRange)
    }

    @Test fun netNeverNegative() {
        val r = Calibration.compute(tank, "0") as DipResult.Ok
        assertEquals(0.0, r.netVolume, 0.0)
    }

    @Test fun reverseLookupRoundTrips() {
        val fwd = Calibration.compute(tank, "10.4") as DipResult.Ok
        val back = Calibration.dipForVolume(tank, fwd.chartVolume.toString()) as DipForVolumeResult.Ok
        assertEquals(10.4, back.dipCm, 1e-6)
    }

    @Test fun reverseLookupAboveMaxIsOutOfRange() {
        assertTrue(Calibration.dipForVolume(tank, "99999") is DipForVolumeResult.OutOfRange)
    }

    @Test fun reverseLookupEmptyAndGarbage() {
        assertTrue(Calibration.dipForVolume(tank, "") is DipForVolumeResult.Empty)
        assertTrue(Calibration.dipForVolume(tank, "abc") is DipForVolumeResult.Invalid)
    }

    @Test fun zeroDipAndMaximumDipAreAccepted() {
        val zero = Calibration.compute(tank, "0") as DipResult.Ok
        assertEquals(0.0, zero.chartVolume, 0.0)
        val max = Calibration.compute(tank, "210") as DipResult.Ok
        assertEquals(volumes.last(), max.chartVolume, 0.0)
    }

    @Test fun negativeDipIsInvalid() {
        assertTrue(Calibration.compute(tank, "-0.1") is DipResult.Invalid)
    }

    @Test fun exactCapacityReverseLookupReturnsMaximumDip() {
        val chartMax = volumes.last()
        val result = Calibration.dipForVolume(tank, chartMax.toString()) as DipForVolumeResult.Ok
        assertEquals(210.0, result.dipCm, 1e-9)
    }

    @Test fun emptyAndGarbage() {
        assertTrue(Calibration.compute(tank, "") is DipResult.Empty)
        assertTrue(Calibration.compute(tank, "1.2.3") is DipResult.Invalid)
    }
}
