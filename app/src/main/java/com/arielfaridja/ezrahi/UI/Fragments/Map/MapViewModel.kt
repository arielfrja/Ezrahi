package com.arielfaridja.ezrahi.UI.Fragments.Map

import androidx.lifecycle.MutableLiveData
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

class MapViewModel : ViewModel() {
    fun addReport(
        title: String,
        description: String,
        location: GeoPoint?,
        type: ReportType = ReportType.GENERAL,
        callback: Callback
    ) {

        db.report_add(
            Report(
                /* actId = */ currentActivity.id,
                /* reporter = */ db.user_getCurrent(),
                /* title = */ title,
                /* description = */ description,
                /* location = */ Latlng(location!!.latitude, location.longitude),
                /* reportTime = */ Date(System.currentTimeMillis()),
                /* reportStatus = */ ReportStatus.REPORTED,
                /* reportType = */ type
            ), callback
        )
    }

    private val db = DataRepoFactory.getInstance()
    val users = MutableLiveData<Map<String, ActUser>>()
    val reports = MutableLiveData<Map<String, Report>>()
    lateinit var currentUser: User
    lateinit var currentActivity: Activity

    init {
        db.user_getAllByCurrentActivity().observeForever { dbUsers ->
            users.value = dbUsers
        }
        db.report_getAllByCurrentActivity().observeForever { dbReports ->
            reports.value = dbReports
        }
        refresh()
    }

    fun refresh() {
        currentUser = db.user_getCurrent()
        currentActivity = db.activity_getCurrent()
    }
//    private val mediator = MediatorLiveData<Map<String,ActUser>>()
//    init {
//        mediator.addSource(db.user_getAllByCurrentActivity()
//        ) {
//                dbUsers -> users.value = dbUsers
//        }
//    }

    // TODO: Implement the ViewModel
}