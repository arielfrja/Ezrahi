package com.arielfaridja.ezrahi.data.mapper

import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportStatus
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

object FieldReportMapper {

    fun fromSnapshot(doc: DocumentSnapshot): FieldReport? {
        if (!doc.exists()) return null
        val firestorePoint = doc.getGeoPoint("Location")
        return FieldReport(
            id = doc.id,
            actId = doc.getString("ActId") ?: "",
            reporterId = doc.getString("ReporterId") ?: "",
            title = doc.getString("Title") ?: "",
            description = doc.getString("Description") ?: "",
            location = GeoPoint(
                latitude = firestorePoint?.latitude ?: 0.0,
                longitude = firestorePoint?.longitude ?: 0.0
            ),
            reportTime = doc.getTimestamp("Time")?.toDate()?.time ?: System.currentTimeMillis(),
            status = FieldReportStatus.getByValue(doc.getDouble("Status")?.toInt() ?: -1),
            type = FieldReportType.getByValue(doc.getDouble("Type")?.toInt() ?: -1),
            typeId = doc.getString("TypeId")
        )
    }

    fun toWriteMap(report: FieldReport): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>(
            "ActId" to report.actId,
            "ReporterId" to report.reporterId,
            "Title" to report.title,
            "Description" to report.description,
            "Location" to FirestoreGeoPoint(report.location.latitude, report.location.longitude),
            "Time" to Timestamp(java.util.Date(report.reportTime)),
            "Status" to report.status.value,
            "Type" to report.type.value
        )
        report.typeId?.let { map["TypeId"] = it }
        return map
    }
}
