package com.arielfaridja.ezrahi.UI.Fragments.Map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.ActUser
import com.arielfaridja.ezrahi.entities.Activity
import com.arielfaridja.ezrahi.entities.Callback
import com.arielfaridja.ezrahi.entities.Latlng
import com.arielfaridja.ezrahi.entities.Report
import com.arielfaridja.ezrahi.entities.ReportStatus
import com.arielfaridja.ezrahi.entities.ReportType
import com.arielfaridja.ezrahi.entities.User
import org.osmdroid.util.GeoPoint
import java.util.Date
import java.util.HashMap

class MapViewModel : ViewModel() {
    private val db = DataRepoFactory.getInstance()

    // Directly expose LiveData from the repository
    val users: LiveData<HashMap<String, ActUser>> = db.user_getAllByCurrentActivity()
    val reports: LiveData<HashMap<String, Report>> = db.report_getAllByCurrentActivity()

    // Expose non-live data directly
    val currentUser: User
        get() = db.user_getCurrent()

    val currentActivity: Activity
        get() = db.activity_getCurrent()

    fun addReport(
        title: String,
        description: String,
        location: GeoPoint?,
        type: ReportType = ReportType.GENERAL,
        callback: Callback<String>
    ) {
        val report = Report(
            actId = currentActivity.id,
            reporter = currentUser,
            title = title,
            description = description,
            location = Latlng(location!!.latitude, location.longitude),
            reportTime = Date(System.currentTimeMillis()),
            reportStatus = ReportStatus.REPORTED,
            reportType = type
        )
        db.report_add(report, callback)
    }
}