package com.arielfaridja.ezrahi.core.mapengine

import android.content.Context
import com.arielfaridja.ezrahi.domain.model.EntityLivenessState
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.model.StalenessConfig
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

object MapLayers {

    fun ensureBaseLayers(style: Style, context: Context) {
        if (style.getSource(PARTICIPANTS_SRC) == null) {
            style.addSource(GeoJsonSource(PARTICIPANTS_SRC, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getLayer(PARTICIPANTS_LAYER) == null) {
            style.addLayer(
                CircleLayer(PARTICIPANTS_LAYER, PARTICIPANTS_SRC).withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(Expression.get("color")),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleOpacity(0.95f)
                )
            )
        }
        if (style.getLayer(PARTICIPANTS_BEARING_LAYER) == null) {
            if (style.getImage("bearing_arrow") == null) {
                style.addImage("bearing_arrow", createArrowBitmap())
            }
            style.addLayer(
                SymbolLayer(PARTICIPANTS_BEARING_LAYER, PARTICIPANTS_SRC).withProperties(
                    PropertyFactory.iconImage("bearing_arrow"),
                    PropertyFactory.iconRotate(Expression.get("bearing")),
                    PropertyFactory.iconRotationAlignment("map"),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconSize(0.8f)
                )
            )
        }
        if (style.getSource(REPORTS_SRC) == null) {
            style.addSource(GeoJsonSource(REPORTS_SRC, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getLayer(REPORTS_LAYER) == null) {
            if (style.getImage("report_general") == null) style.addImage("report_general", createReportBitmap(false))
            if (style.getImage("report_medical") == null) style.addImage("report_medical", createReportBitmap(true))
            style.addLayer(
                SymbolLayer(REPORTS_LAYER, REPORTS_SRC).withProperties(
                    PropertyFactory.iconImage(Expression.get("icon")),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
            )
        }
        if (style.getSource(ROUTE_SRC) == null) {
            style.addSource(GeoJsonSource(ROUTE_SRC, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getLayer(ROUTE_LAYER) == null) {
            style.addLayerBelow(
                LineLayer(ROUTE_LAYER, ROUTE_SRC).withProperties(
                    PropertyFactory.lineColor("#1565C0"),
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.9f)
                ),
                PARTICIPANTS_LAYER
            )
        }
    }

    fun updateParticipants(style: Style, participants: List<EventParticipant>, config: StalenessConfig) {
        val source = style.getSourceAs<GeoJsonSource>(PARTICIPANTS_SRC) ?: return
        val now = System.currentTimeMillis()
        val features = participants.mapNotNull { participant ->
            val location = participant.currentLocation ?: return@mapNotNull null
            if (participant.effectiveState(config, now) == EntityLivenessState.EXPIRED) return@mapNotNull null
            val state = participant.effectiveState(config, now)
            Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude)).also {
                it.addStringProperty("name", participant.fullName)
                it.addStringProperty("color", MapLibreConfig.livenessColor(state))
                it.addNumberProperty("bearing", 0.0)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    fun updateReports(style: Style, reports: List<FieldReport>) {
        val source = style.getSourceAs<GeoJsonSource>(REPORTS_SRC) ?: return
        val features = reports.mapNotNull { report ->
            val location = report.location ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude)).also {
                it.addStringProperty("title", report.title.ifEmpty { "Report" })
                it.addStringProperty(
                    "icon",
                    if (report.type == FieldReportType.MEDICAL) "report_medical" else "report_general"
                )
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    fun updateRoute(style: Style, points: List<GeoPoint>) {
        val source = style.getSourceAs<GeoJsonSource>(ROUTE_SRC) ?: return
        val linePoints = points.map { Point.fromLngLat(it.longitude, it.latitude) }
        if (linePoints.size < 2) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(LineString.fromLngLats(linePoints)))))
    }

    fun ensureMeasureLayers(style: Style) {
        if (style.getSource(MEASURE_SRC) == null) {
            style.addSource(GeoJsonSource(MEASURE_SRC, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getLayer(MEASURE_LINE_LAYER) == null) {
            style.addLayerBelow(
                LineLayer(MEASURE_LINE_LAYER, MEASURE_SRC).withProperties(
                    PropertyFactory.lineColor("#6A1B9A"),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineOpacity(0.9f)
                ),
                PARTICIPANTS_LAYER
            )
        }
        if (style.getLayer(MEASURE_POINTS_LAYER) == null) {
            style.addLayerBelow(
                CircleLayer(MEASURE_POINTS_LAYER, MEASURE_SRC).withProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleColor("#6A1B9A"),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                    PropertyFactory.circleStrokeWidth(2f)
                ),
                PARTICIPANTS_LAYER
            )
        }
    }

    fun updateMeasure(style: Style, points: List<GeoPoint>) {
        val source = style.getSourceAs<GeoJsonSource>(MEASURE_SRC) ?: return
        val features = mutableListOf<Feature>()
        if (points.size >= 2) {
            val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.longitude, it.latitude) })
            features.add(Feature.fromGeometry(line))
        }
        points.forEach { p ->
            features.add(Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude)))
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}
