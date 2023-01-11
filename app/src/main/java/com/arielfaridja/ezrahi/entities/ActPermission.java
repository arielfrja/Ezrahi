//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Map;

@Entity(tableName = "permissions")
public class ActPermission {
    private String ActId;

    @PrimaryKey
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

    @NonNull
    public static ActPermission mapToPermission(Map<String, Object> permissions, int level) {
        return new ActPermission(
                level,
                (String) permissions.get("Name"),
                (Boolean) permissions.get("showHigher"),
                (Boolean) permissions.get("showLower"),
                (Boolean) permissions.get("showRoute"),
                (Boolean) permissions.get("addUser"),
                (Boolean) permissions.get("removeUser"),
                (Boolean) permissions.get("showHigherReports"),
                (Boolean) permissions.get("showLowerReports"),
                (Boolean) permissions.get("canReport")
        );
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

    public String getActId() {
        return this.ActId;
    }

    public void setActId(String actId) {
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
