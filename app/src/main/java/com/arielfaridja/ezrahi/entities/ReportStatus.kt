package com.arielfaridja.ezrahi.entities

enum class ReportStatus(val value: Int) {
    REPORTED(1),
    HANDLED(2),
    UNKNOWN(-1);

    companion object {
        fun getByValue(value: Int): ReportStatus = when (value) {
            1 -> REPORTED
            2 -> HANDLED
            else -> UNKNOWN
        }
    }
}
