package com.arielfaridja.ezrahi.app.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

enum class CoordFormat(val label: String) {
    ITM("ITM"),
    MGRS("MGRS"),
    WGS84("WGS84");

    fun next(): CoordFormat = entries[(ordinal + 1) % entries.size]
}

object Coordinates {

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * Math.atan2(sqrt(a), sqrt(1 - a))
    }

    fun format(lat: Double, lon: Double, format: CoordFormat): String = when (format) {
        CoordFormat.WGS84 -> toDms(lat, lon)
        CoordFormat.ITM -> {
            val (e, n) = toItm(lat, lon)
            "E ${e.toInt()} N ${n.toInt()}"
        }
        CoordFormat.MGRS -> toMgrs(lat, lon, 5)
    }

    fun toDms(lat: Double, lon: Double): String {
        return "${dms(abs(lat), if (lat >= 0) "N" else "S")} ${dms(abs(lon), if (lon >= 0) "E" else "W")}"
    }

    private fun dms(value: Double, hemisphere: String): String {
        val d = floor(value)
        val mFull = (value - d) * 60
        val m = floor(mFull)
        val s = (mFull - m) * 60
        return "%d°%02d'%04.1f\"%s".format(d.toInt(), m.toInt(), s, hemisphere)
    }

    fun toItm(lat: Double, lon: Double): Pair<Double, Double> {
        return transverseMercator(
            lat = lat,
            lon = lon,
            k0 = 1.0000067,
            lon0Deg = 35.20451694444445,
            falseEasting = 500000.0,
            falseNorthing = 5000000.0,
            a = 6378137.0,
            invF = 298.257222101
        )
    }

    private data class UtmResult(val zone: Int, val easting: Double, val northing: Double)

    fun toMgrs(lat: Double, lon: Double, precision: Int): String {
        val zone = floor((lon + 180) / 6).toInt() + 1
        val lon0 = (zone - 1) * 6 - 180 + 3
        val tm = transverseMercator(
            lat = lat,
            lon = lon,
            k0 = 0.9996,
            lon0Deg = lon0.toDouble(),
            falseEasting = 500000.0,
            falseNorthing = if (lat < 0) 10000000.0 else 0.0,
            a = 6378137.0,
            invF = 298.257222101
        )
        val set = ((zone - 1) % 6) + 1
        val colLetters = when (set) {
            1, 4 -> "ABCDEFGH"
            2, 5 -> "JKLMNPQR"
            else -> "STUVWXYZ"
        }
        val rowLetters = "ABCDEFGHJKLMNPQRSTUV"
        val rowOriginOffset = if (zone % 2 == 0) 5 else 0

        val colIndex = floor(tm.first / 100000).toInt().coerceIn(0, colLetters.length - 1)
        val rowIndex = (floor(tm.second / 100000).toInt() % 20 + 20) % 20
        val e = tm.first % 100000
        val n = tm.second % 100000

        val bandIdx = floor((lat + 80) / 8).toInt().coerceIn(0, "CDEFGHJKLMNPQRSTUVWX".length - 1)
        val band = "CDEFGHJKLMNPQRSTUVWX"[bandIdx]

        val digits = precision.coerceAtMost(5)
        val divisor = 10.0.pow(5 - digits)
        val fmt = "%0${digits}d"

        return buildString {
            append(zone).append(band).append(' ')
            append(colLetters[colIndex]).append(rowLetters[(rowIndex + rowOriginOffset) % 20]).append(' ')
            append(fmt.format(floor(e / divisor).toInt())).append(' ')
            append(fmt.format(floor(n / divisor).toInt()))
        }
    }

    private fun transverseMercator(
        lat: Double,
        lon: Double,
        k0: Double,
        lon0Deg: Double,
        falseEasting: Double,
        falseNorthing: Double,
        a: Double,
        invF: Double
    ): Pair<Double, Double> {
        val f = 1.0 / invF
        val e2 = 2 * f - f * f
        val ep2 = e2 / (1 - e2)

        val phi = Math.toRadians(lat)
        val lambda = Math.toRadians(lon)
        val lambda0 = Math.toRadians(lon0Deg)

        val n = a / sqrt(1 - e2 * sin(phi) * sin(phi))
        val t = tan(phi) * tan(phi)
        val c = ep2 * cos(phi) * cos(phi)
        val aa = (lambda - lambda0) * cos(phi)

        val m = a * (
            (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2.pow(3) / 256) * phi -
                (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2.pow(3) / 1024) * sin(2 * phi) +
                (15 * e2 * e2 / 256 + 45 * e2.pow(3) / 1024) * sin(4 * phi) -
                (35 * e2.pow(3) / 3072) * sin(6 * phi)
            )

        val easting = falseEasting + k0 * n * (
            aa + (1 - t + c) * aa.pow(3) / 6 +
                (5 - 18 * t + t * t + 72 * c - 58 * ep2) * aa.pow(5) / 120
            )

        val northing = falseNorthing + k0 * (
            m + n * tan(phi) * (
                aa * aa / 2 +
                    (5 - t + 9 * c + 4 * c * c) * aa.pow(4) / 24 +
                    (61 - 58 * t + t * t + 600 * c - 330 * ep2) * aa.pow(6) / 720
                )
            )

        return Pair(easting, northing)
    }
}
