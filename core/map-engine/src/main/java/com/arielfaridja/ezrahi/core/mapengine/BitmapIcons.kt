package com.arielfaridja.ezrahi.core.mapengine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

fun createArrowBitmap(): Bitmap {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val path = Path().apply {
        moveTo(size / 2f, 4f)
        lineTo(size - 8f, size / 2f)
        lineTo(size / 2f, size - 10f)
        lineTo(8f, size / 2f)
        close()
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawPath(path, stroke)
    return bitmap
}

fun createReportBitmap(medical: Boolean): Bitmap {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (medical) Color.parseColor("#D32F2F") else Color.parseColor("#1565C0")
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, fill)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, ring)
    if (medical) {
        val cross = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(size / 2f, 14f, size / 2f, size - 14f, cross)
        canvas.drawLine(14f, size / 2f, size - 14f, size / 2f, cross)
    }
    return bitmap
}
