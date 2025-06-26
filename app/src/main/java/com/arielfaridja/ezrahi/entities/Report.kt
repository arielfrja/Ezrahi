package com.arielfaridja.ezrahi.entities

import java.util.Date

class Report() {
    var id: String? = null
    var description: String? = null
    var title: String? = null
    var actId: String? = null
    var reporter: User? = null
    var location: Latlng? = null
    var reportTime: Date? = null
    var reportStatus: ReportStatus? = null
    var reportType: ReportType? = null

    constructor(
        actId: String?,
        reporter: User?,
        title: String?,
        description: String?,
        location: Latlng?,
        reportTime: Date?,
        reportStatus: ReportStatus?,
        reportType: ReportType?,
        id: String? = null
    ) : this() {
        this.actId = actId
        this.reporter = reporter
        this.title = title
        this.description = description
        this.location = location
        this.reportTime = reportTime
        this.reportStatus = reportStatus
        this.reportType = reportType
        this.id = id
    }
}
