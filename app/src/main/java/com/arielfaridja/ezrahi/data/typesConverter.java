package com.arielfaridja.ezrahi.data;

import androidx.room.TypeConverter;

import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;
import com.google.gson.Gson;

import org.json.JSONException;

public class typesConverter {
    @TypeConverter
    public static String userToString(User user) {
        return user == null ? null : user.toString();
    }

    @TypeConverter
    public static User stringToUser(String string) throws JSONException {
        User u;
        Gson gson = new Gson();
        u = gson.fromJson(string, User.class);
        return u;
    }

    @TypeConverter
    public static Latlng stringToLatlng(String string) throws JSONException {
        return Latlng.StringToLatlng(string);
    }

    @TypeConverter
    public static String latlngToString(Latlng latlng) {
        return latlng.toString();
    }
}
