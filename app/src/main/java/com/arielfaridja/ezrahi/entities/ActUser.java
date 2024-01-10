//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;


@Entity(primaryKeys = {"id"}, tableName = "Users")
public class ActUser extends User {
    private String actId;
    private int role;

    public ActUser(String id, String phone, String email, String firstName, String lastName, Latlng location, String actId, int role) {
        super(id, phone, email, firstName, lastName, location);
        this.actId = actId;
        this.role = role;
    }

    public ActUser(String actId, User u, int role) {
        super(u);
        this.actId = actId;
        this.role = role;
    }

    public ActUser(String actId, String userId, int role) {
        super();
        this.setId(userId);
        this.actId = actId;
        this.role = role;
    }

    public String getActId() {
        return this.actId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public int getRole() {
        return this.role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public void setUser(@NonNull User u) {
        setId(u.id);
        setEmail(u.email);
        setFirstName(u.firstName);
        setLastName(u.lastName);
        setLocation(u.location);
        setPhone(u.phone);
    }
}
