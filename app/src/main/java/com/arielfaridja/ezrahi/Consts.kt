package com.arielfaridja.ezrahi

import com.arielfaridja.ezrahi.UI.ExtensionMethods.Companion.toInt
import com.arielfaridja.ezrahi.entities.ReportType
object Consts {

    val iconTextArray: Array<Triple<Int, Int, Int>> =
        //the second element of the triple is a drawable resource id.
        arrayOf(
            Triple(
                (R.drawable.report_canvas),
                R.string.general, ReportType.GENERAL.toInt()
            ),
            Triple(
                R.drawable.report_medical,
                R.string.medical, ReportType.MEDICAL.toInt()
            )
        )
}


