//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

public class ActPermission {
    private int ActId;
    private int level;
    private String name;
    private Boolean showHigher;
    private Boolean showLower;
    private Boolean showRoute;
    private Boolean addUser;
    private Boolean removeUser;

    private Boolean showHigherReports;
    private Boolean showLowerReports;
    private Boolean canReport;

    public ActPermission(int level, String name, Boolean showHigher, Boolean showLower, Boolean showRoute, Boolean addUser, Boolean removeUser, Boolean showHigherReports, Boolean showLowerReports, Boolean canReport) {
        this.level = level;
        this.name = name;
        this.showHigher = showHigher;
        this.showLower = showLower;
        this.showRoute = showRoute;
        this.addUser = addUser;
        this.removeUser = removeUser;
        this.showHigherReports = showHigherReports;
        this.showLowerReports = showLowerReports;
        this.canReport = canReport;
    }

    public Boolean getShowHigherReports() {
        return showHigherReports;
    }

    public void setShowHigherReports(Boolean showHigherReports) {
        this.showHigherReports = showHigherReports;
    }

    public Boolean getShowLowerReports() {
        return showLowerReports;
    }

    public void setShowLowerReports(Boolean showLowerReports) {
        this.showLowerReports = showLowerReports;
    }

    public Boolean getCanReport() {
        return canReport;
    }

    public void setCanReport(Boolean canReport) {
        this.canReport = canReport;
    }

    public int getActId() {
        return this.ActId;
    }

    public void setActId(int actId) {
        this.ActId = actId;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getShowHigher() {
        return this.showHigher;
    }

    public void setShowHigher(Boolean showHigher) {
        this.showHigher = showHigher;
    }

    public Boolean getShowLower() {
        return this.showLower;
    }

    public void setShowLower(Boolean showLower) {
        this.showLower = showLower;
    }

    public Boolean getShowRoute() {
        return this.showRoute;
    }

    public void setShowRoute(Boolean showRoute) {
        this.showRoute = showRoute;
    }

    public Boolean getAddUser() {
        return this.addUser;
    }

    public void setAddUser(Boolean addUser) {
        this.addUser = addUser;
    }

    public Boolean getRemoveUser() {
        return this.removeUser;
    }

    public void setRemoveUser(Boolean removeUser) {
        this.removeUser = removeUser;
    }
}
