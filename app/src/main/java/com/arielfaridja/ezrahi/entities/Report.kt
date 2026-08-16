package com.arielfaridja.ezrahi.entities

import java.util.Date

class Report() {
    var id: String = ""
    var description: String = ""
    var title: String = ""
    var actId: String = ""
    var reporter: User = User()
    var location: Latlng = Latlng()
    var reportTime: Date = Date()
    var reportStatus: ReportStatus = ReportStatus.REPORTED
    var reportType: ReportType = ReportType.UNKNOWN

    constructor(
        actId: String,
        reporter: User,
        title: String,
        description: String,
        location: Latlng,
        reportTime: Date,
        reportStatus: ReportStatus,
        reportType: ReportType,
        id: String = ""
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
