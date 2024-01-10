package com.arielfaridja.ezrahi.UI.Fragments.Map

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.ActUser
import com.arielfaridja.ezrahi.entities.Callback
import com.arielfaridja.ezrahi.entities.Report
import com.arielfaridja.ezrahi.entities.ReportStatus
import com.arielfaridja.ezrahi.entities.ReportType
import org.osmdroid.util.GeoPoint
import java.util.Date

class MapViewModel : ViewModel() {
    fun addReport(title: String, description: String, location: GeoPoint?, callback: Callback) {
        db.report_add(
            Report(
                currentActivity.id,
                db.user_getCurrent(),
                title,
                description,
                currentUser.location,
                Date(System.currentTimeMillis()),
                ReportStatus.REPORTED,
                ReportType.GENERAL
            ), callback
        )
    }

    private val db = DataRepoFactory.getInstance()
    val users = MutableLiveData<Map<String, ActUser>>()
    val currentUser = db.user_getCurrent()
    val currentActivity = db.activity_getCurrent()

    init {
        db.user_getAllByCurrentActivity().observeForever { dbUsers ->
            users.value = dbUsers
        }
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