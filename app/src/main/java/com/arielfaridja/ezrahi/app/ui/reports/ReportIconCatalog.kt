package com.arielfaridja.ezrahi.app.ui.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.arielfaridja.ezrahi.R

/**
 * Single-source-of-truth icon catalog for dynamic report types
 * (spec docs/specs/dynamic-report-types.md §6).
 *
 * All icons live as official Android VectorDrawables in res/drawable/ic_rtype_*.xml.
 * - Compose UI uses painterResource(entry.resId) with tint = entry.accent.
 * - MapLibre markers use renderMarkerBitmap(context, iconKey) to produce ARGB Bitmaps.
 */
data class ReportIconEntry(@DrawableRes val resId: Int, val accent: Color)

object ReportIconCatalog {
    val entries: Map<String, ReportIconEntry> = mapOf(
        "general" to ReportIconEntry(R.drawable.ic_rtype_info, Color(0xFF2E7D32)),
        "medical" to ReportIconEntry(R.drawable.ic_rtype_star_of_david, Color(0xFFC62828)),
        "hazard" to ReportIconEntry(R.drawable.ic_rtype_warning, Color(0xFFF9A825)),
        "fire" to ReportIconEntry(R.drawable.ic_rtype_local_fire_department, Color(0xFFE64A19)),
        "water" to ReportIconEntry(R.drawable.ic_rtype_water_drop, Color(0xFF1565C0)),
        "tree" to ReportIconEntry(R.drawable.ic_rtype_forest, Color(0xFF33691E)),
        "road" to ReportIconEntry(R.drawable.ic_rtype_road, Color(0xFF795548)),
        "trail" to ReportIconEntry(R.drawable.ic_rtype_hiking, Color(0xFF00796B)),
        "food" to ReportIconEntry(R.drawable.ic_rtype_restaurant, Color(0xFFEF6C00)),
        "meeting" to ReportIconEntry(R.drawable.ic_rtype_groups, Color(0xFF3949AB)),
        "vehicle" to ReportIconEntry(R.drawable.ic_rtype_directions_car, Color(0xFF455A64)),
        "weather" to ReportIconEntry(R.drawable.ic_rtype_cloud, Color(0xFF0288D1)),
        "lost_found" to ReportIconEntry(R.drawable.ic_rtype_person_search, Color(0xFFAD1457)),
        "checkpoint" to ReportIconEntry(R.drawable.ic_rtype_flag, Color(0xFF5E35B1))
    )

    private val fallback = entries.getValue("general")

    fun entry(iconKey: String): ReportIconEntry = entries[iconKey] ?: fallback

    /**
     * Renders the catalog glyph as a circular map-marker bitmap
     * (accent-colored disc, white ring, white glyph). Registered by
     * MapLayers.ensureReportTypeIcons under the "rtype_<iconKey>" names.
     */
    fun renderMarkerBitmap(context: Context, iconKey: String, colorHex: String? = null, sizePx: Int = 48): Bitmap {
        val key = if (entries.containsKey(iconKey)) iconKey else "general"
        val entry = entries.getValue(key)
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, entry.resId)
            ?: throw IllegalStateException("Missing drawable for report type $iconKey")
        val glyphSize = (sizePx * 0.62f).toInt()
        val offset = ((sizePx - glyphSize) / 2f).toInt()
        drawable.setBounds(offset, offset, offset + glyphSize, offset + glyphSize)
        drawable.setTint(android.graphics.Color.WHITE)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val discColor = colorHex?.let { runCatching { android.graphics.Color.parseColor(it) }.getOrNull() }
            ?: entry.accent.toArgb()
        val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = discColor }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 4f, disc)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 4f, ring)
        drawable.draw(canvas)
        return bitmap
    }
}
