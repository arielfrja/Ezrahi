//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

import org.json.JSONException;
import org.json.JSONObject;

public class Latlng {
    private double latitude;
    private double longitude;

    public Latlng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Latlng() {
        this.latitude = 0.0D;
        this.longitude = 0.0D;
    }

    public Latlng(Latlng latlng) {
        this.latitude = latlng.getLatitude();
        this.longitude = latlng.getLongitude();
    }

    public Latlng(GeoPoint point) {
        this.latitude = point.getLatitude();
        this.longitude = point.getLongitude();
    }

    public static Latlng StringToLatlng(String string) throws JSONException {
        Latlng res = new Latlng();
        JSONObject j = new JSONObject('{' + string + '}');
        j = new JSONObject(j.get("Latlng").toString());
        res.setLatitude(Double.parseDouble(j.get("latitude").toString()));
        res.setLongitude(Double.parseDouble(j.get("longitude").toString()));
        return res;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @NonNull
    @Override
    public String toString() {
        return "Latlng: {latitude=" + this.latitude + ", longitude=" + this.longitude + '}';
    }
}
