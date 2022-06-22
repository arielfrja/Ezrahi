//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

public class ActUser extends User {
    private String actId;
    private int permission;
    private int userId;

    public ActUser(String id, String phone, String email, String firstName, String lastName, Latlng location, String actId, int permission) {
        super(id, phone, email, firstName, lastName, location);
        this.actId = actId;
        this.permission = permission;
    }

    public ActUser(String actId, User u, int permission) {
        super(u);
        this.actId = actId;
        this.permission = permission;
    }

    public String getActId() {
        return this.actId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public int getPermission() {
        return this.permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }
}
