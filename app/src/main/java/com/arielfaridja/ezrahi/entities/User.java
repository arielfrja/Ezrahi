//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    @NonNull
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
        this.phone = phone;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.location = location;
    }

    public User() {
        this.id = "";
        this.phone = "";
        this.email = "";
        this.firstName = "";
        this.lastName = "";
        this.location = new Latlng();
    }

    static public User MapToUser(Map<String, Object> map) {
        String id = (String) map.get("uId");
        String firstName = (String) map.get("FirstName");
        String lastName = (String) map.get("LastName");
        String phone = (String) map.get("Email");
        String email = (String) map.get("Latitude");
        double latitude;
        double longitude;
        if (map.containsKey("Latitude"))
            latitude = (double) map.get("Latitude");
        else
            latitude = 0.0;
        if (map.containsKey("Longitude"))
            longitude = (double) map.get("Longitude");
        else
            longitude = 0.0;

        return new User(id, firstName, lastName, phone, email, new Latlng(latitude, longitude));

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

    public HashMap toHashMap() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("uId", this.id);
        data.put("FirstName", this.firstName);
        data.put("LastName", this.lastName);
        data.put("Email", this.email);
        data.put("Latitude", this.location.getLatitude());
        data.put("Longitude", this.location.getLongitude());
        data.put("Phone", this.phone);

        return data;
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);

        //return "User{id='" + this.id + '\'' + ", firstName='" + this.firstName + '\'' + ", lastName='" + this.lastName + '\'' + ", email='" + this.email + '\'' + ", phone='" + this.phone + '\'' + ", location=" + this.location + '}';
    }

}
