package com.arielfaridja.ezrahi.entities;

import java.util.Date;

public class Report {
    private String id;
    private String description;
    private String title;
    private String actId;
    private User reporter;
    private Latlng location;
    private Date reportTime;
    private ReportStatus reportStatus;
    private ReportType reportType;

    public Report() {
        // Empty constructor
    }

    public Report(String actId, User reporter, String title, String description, Latlng location, Date reportTime, ReportStatus reportStatus, ReportType reportType, String id) {
        this.title = title;
        this.description = description;
        this.actId = actId;
        this.reporter = reporter;
        this.location = location;
        this.reportTime = reportTime;
        this.reportStatus = reportStatus;
        this.reportType = reportType;
        this.id = id;
    }

    public Report(String actId, User reporter, String title, String description, Latlng location, Date reportTime, ReportStatus reportStatus, ReportType reportType) {
        this.title = title;
        this.description = description;
        this.actId = actId;
        this.reporter = reporter;
        this.location = location;
        this.reportTime = reportTime;
        this.reportStatus = reportStatus;
        this.reportType = reportType;

    }

    public String getActId() {
        return actId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Latlng getLocation() {
        return location;
    }

    public void setLocation(Latlng location) {
        this.location = location;
    }

    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(ReportStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    public Date getReportTime() {
        return reportTime;
    }

    public void setReportTime(Date reportTime) {
        this.reportTime = reportTime;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
