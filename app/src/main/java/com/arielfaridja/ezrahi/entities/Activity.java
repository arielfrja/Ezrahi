//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import java.util.ArrayList;

public class Activity {
    private String id;
    private String name;
    private ArrayList<String> routesSrc;
    private User owner;
    private ArrayList<ActUser> users;
    private ArrayList<ActPermission> permissions;

    public Activity(String id, String name, ArrayList<String> routesSrc, User owner, ArrayList<ActUser> users, ArrayList<ActPermission> permissions) {
        this.id = id;
        this.name = name;
        this.routesSrc = routesSrc;
        this.owner = owner;
        this.users = users;
        this.permissions = permissions;
    }

    public Activity() {

    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getRoutesSrc() {
        return this.routesSrc;
    }

    public void setRoutesSrc(ArrayList<String> routesSrc) {
        this.routesSrc = routesSrc;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public ArrayList<ActUser> getUsers() {
        return this.users;
    }

    public void setUsers(ArrayList<ActUser> users) {
        this.users = users;
    }

    public ArrayList<ActPermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(ArrayList<ActPermission> permissions) {
        this.permissions = permissions;
    }
}
