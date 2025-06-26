//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Map;

@Entity(tableName = "permissions")
public class ActPermission {
    private String ActId;

    @PrimaryKey
    private int id;
    private String name;
    private Boolean showRoute;
    private Boolean addUser;
    private Boolean removeUser;
    private ArrayList<Integer> rolesToHide;
    private ArrayList<Integer> reportsToHide;

    private Boolean canReport;

    public ActPermission(int id, String name, Boolean showRoute, Boolean addUser, Boolean removeUser, Boolean canReport, ArrayList<Integer> rolesToHide, ArrayList<Integer> reportsToHide) {
        this.id = id;
        this.name = name;
        this.showRoute = showRoute;
        this.addUser = addUser;
        this.removeUser = removeUser;
        this.canReport = canReport;
        this.rolesToHide = rolesToHide;
        this.reportsToHide = reportsToHide;
    }

    @NonNull
    public static ActPermission mapToPermission(Map<String, Object> permissions, int level) {
        return new ActPermission(
                level,
                (String) permissions.get("Name"),
                (Boolean) permissions.get("ShowRoute"),
                (Boolean) permissions.get("AddUser"),
                (Boolean) permissions.get("RemoveUser"),
                (Boolean) permissions.get("CanReport"),
                (ArrayList<Integer>) permissions.get("RolesToHide"),
                (ArrayList<Integer>) permissions.get("ReportsToHide")
        );
    }

    public Boolean getCanReport() {
        return canReport;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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

    public ArrayList<Integer> getReportsToHide() {
        return reportsToHide;
    }

    public void setReportsToHide(ArrayList<Integer> reportsToHide) {
        this.reportsToHide = reportsToHide;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getShowRoute() {
        return this.showRoute;
    }

    public ArrayList<Integer> getRolesToHide() {
        return rolesToHide;
    }

    public void setRolesToHide(ArrayList<Integer> rolesToHide) {
        this.rolesToHide = rolesToHide;
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
