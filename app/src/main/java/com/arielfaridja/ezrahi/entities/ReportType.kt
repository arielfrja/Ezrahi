package com.arielfaridja.ezrahi.entities

enum class ReportType(val value: Int) {
    GENERAL(0),
    MEDICAL(1),
    UNKNOWN(-1);

    companion object {
        fun getByValue(value: Int): ReportType = when (value) {
            0 -> GENERAL
            1 -> MEDICAL
            else -> UNKNOWN
        }
    }
}
