//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import java.io.Serializable;

public class User implements Serializable {
    protected String id;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String phone;
    protected Latlng location;

    public User(User u) {
        this.id = u.getId();
        this.phone = u.getPhone();
        this.email = u.getEmail();
        this.firstName = u.getFirstName();
        this.lastName = u.getLastName();
        this.location = u.getLocation();
    }

    public User(String id, String firstName, String lastName, String phone, String email, Latlng location) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.location = location;
    }
    public User copy(User user)
    {
        this.id = user.id;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.phone = user.phone;
        this.email = user.email;
        this.location = user.location;
        return this;
    }
    public User() {
        this.id = "";
        this.firstName = "";
        this.lastName = "";
        this.phone = "";
        this.email = "";
        this.location = new Latlng();
    }

    public String toString() {
        return "User{id='" + this.id + '\'' + ", firstName='" + this.firstName + '\'' + ", lastName='" + this.lastName + '\'' + ", email='" + this.email + '\'' + ", phone='" + this.phone + '\'' + ", location=" + this.location + '}';
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Latlng getLocation() {
        return this.location;
    }

    public void setLocation(Latlng location) {
        this.location = location;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
