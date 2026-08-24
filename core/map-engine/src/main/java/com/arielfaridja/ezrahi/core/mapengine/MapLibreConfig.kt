package com.arielfaridja.ezrahi.core.mapengine

import com.arielfaridja.ezrahi.domain.model.EntityLivenessState

const val PARTICIPANTS_SRC = "ezrahi-participants-src"
const val PARTICIPANTS_LAYER = "ezrahi-participants-layer"
const val PARTICIPANTS_BEARING_LAYER = "ezrahi-participants-bearing-layer"
const val REPORTS_SRC = "ezrahi-reports-src"
const val REPORTS_LAYER = "ezrahi-reports-layer"
const val ROUTE_SRC = "ezrahi-route-src"
const val ROUTE_LAYER = "ezrahi-route-layer"
const val MEASURE_SRC = "ezrahi-measure-src"
const val MEASURE_LINE_LAYER = "ezrahi-measure-line-layer"
const val MEASURE_POINTS_LAYER = "ezrahi-measure-points-layer"

object MapLibreConfig {
    const val ONLINE_STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"

    fun livenessColor(state: EntityLivenessState): String = when (state) {
        EntityLivenessState.ACTIVE -> "#2E7D32"
        EntityLivenessState.STALE -> "#F9A825"
        EntityLivenessState.DISCONNECTED -> "#616161"
        EntityLivenessState.EXPIRED -> "#9E9E9E"
    }
}
