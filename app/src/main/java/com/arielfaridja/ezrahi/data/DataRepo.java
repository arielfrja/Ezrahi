package com.arielfaridja.ezrahi.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.Callback;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;

import org.json.JSONArray;

public interface DataRepo {
    Boolean user_add(User user);

    Boolean user_remove(String uId);

    Boolean user_update(User user);

    MutableLiveData<User> user_get(String user, Callback callback);

    JSONArray user_getAll();

    Latlng user_getLocation(String uId);

    void auth_email_user_register(User user, String password);

    LiveData<User> auth_email_user_login(String username, String password, Callback callback);

    MutableLiveData<Activity> activity_get(String actId, Callback callback);

    Activity activity_getCurrent();

    Boolean user_isSignedIn();
}