/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arielfaridja.ezrahi.UI

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.drawable.Drawable

class TextDrawable(
    private val mText: String,
    val color: Int,
    val backgroundColor: Int,
    private val textSize: Float
) :
    Drawable() {
    private val mPaint: Paint
    private val mIntrinsicWidth: Int
    private val mIntrinsicHeight: Int
    private val bgPaint: Paint

    init {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = DEFAULT_COLOR
        mPaint.textAlign = Align.CENTER

        mPaint.textSize = textSize
        mIntrinsicWidth = (mPaint.measureText(mText, 0, mText.length) + .5).toInt()
        mIntrinsicHeight = mPaint.getFontMetricsInt(null)
        bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = backgroundColor
        bgPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        canvas.drawRect(bounds, bgPaint)
        canvas.drawText(
            mText, 0, mText.length,
            bounds.centerX().toFloat(), bounds.centerY().toFloat(), mPaint
        )
    }

    override fun getOpacity(): Int {
        return mPaint.alpha
    }

    override fun getIntrinsicWidth(): Int {
        return mIntrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mIntrinsicHeight
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(filter: ColorFilter?) {
        mPaint.colorFilter = filter
    }

    companion object {
        private const val DEFAULT_COLOR = Color.WHITE
        private const val DEFAULT_TEXTSIZE = 15
    }
}