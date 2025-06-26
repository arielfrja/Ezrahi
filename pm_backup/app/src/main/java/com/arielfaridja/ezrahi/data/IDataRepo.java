package com.arielfaridja.ezrahi.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arielfaridja.ezrahi.entities.ActUser;
import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.Callback;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.Report;
import com.arielfaridja.ezrahi.entities.User;

import org.json.JSONArray;

import java.util.HashMap;

public interface IDataRepo {

    /***
     * a method to get an activity by id, from the server.
     * @param actId The id of the wanted activity
     * @param callback A function to call after you get (or not) the activity/
     * @return
     */
    MutableLiveData<Activity> activity_get(String actId, Callback callback);

    /***
     *
     * @return
     */
    MutableLiveData<HashMap<String, Activity>> activity_getAllByCurrentUser();


    void activity_getAllByUser(String uId, Callback callback);

    Activity activity_getCurrent();

    void activity_setCurrent(String actId);

    LiveData<User> auth_email_user_login(String username, String password, Callback callback);

    void auth_email_user_register(User user, String password, Callback callback);

    Boolean user_add(User user);

    MutableLiveData<User> user_get(String uId, Callback callback);

    JSONArray user_getAll();

    HashMap<String, ActUser> user_getAllByActivity(String actId, Callback callback);

    void report_getAllByActivity(String actId, Callback callback);

    void report_add(Report report, Callback callback);

    MutableLiveData<HashMap<String, Report>> report_getAllByCurrentActivity();

    MutableLiveData<HashMap<String, ActUser>> user_getAllByCurrentActivity();

    User user_getCurrent();

    Latlng user_getLocation(String uId);

    Boolean user_isSignedIn();

    Boolean user_remove(String uId);

    void user_setCurrent(String uId);

    Boolean user_update(User user);
}