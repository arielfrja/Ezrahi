package com.arielfaridja.ezrahi.UI.Main;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.data.IDataRepo;
import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;

import java.util.HashMap;

public class MainActivityViewModel extends ViewModel {


    IDataRepo dataRepo = DataRepoFactory.getInstance();
    Intent intent;
    MutableLiveData<Boolean> isSignIn = new MutableLiveData<>(null);
    MutableLiveData<Activity> currentActivity = new MutableLiveData<>(dataRepo.activity_getCurrent());
    private User user;

    public MainActivityViewModel() {
        super();
        getIsSignIn().setValue(dataRepo.user_isSignedIn());
        user = dataRepo.user_getCurrent();

    }

    public MutableLiveData<Boolean> getIsSignIn() {
        return isSignIn;
    }

    public void setIsSignIn(MutableLiveData<Boolean> isSignIn) {
        this.isSignIn = isSignIn;
    }

    public Activity getActivity() {
        return currentActivity.getValue();
    }

    public void setActivity(Activity activity) {
        currentActivity.setValue(activity);
    }

    public MutableLiveData<Activity> getCurrentActivity() {
        return currentActivity;
    }

    MutableLiveData<HashMap<String, Activity>> getCurrentUsersActivities() {
        return dataRepo.activity_getAllByCurrentUser();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    void init(Intent intent) {
        initUser(intent);
    }

    void initUser(Intent intent) {
        this.user = new User(intent.getStringExtra("id"),
                intent.getStringExtra("firstName"),
                intent.getStringExtra("lastName"),
                intent.getStringExtra("email"),
                intent.getStringExtra("phone"),
                new Latlng(intent.getDoubleExtra("longitude", 32.0D), intent.getDoubleExtra("latitude", 34.0D)));
    }

    public void isUserSignedIn() {
        if (!dataRepo.user_isSignedIn())
            isSignIn.setValue(false);
        else
            isSignIn.setValue(true);
    }

    public void loadActivityDataFromSP(SharedPreferences sp) {
        String actId = sp.getString("actId", "");
        if (!actId.isEmpty()) {
            dataRepo.activity_setCurrent(actId);
            new Thread(() -> {
                while (dataRepo.activity_getCurrent() == null) ;
                currentActivity.setValue(dataRepo.activity_getCurrent());
            }).start();
            //TODO: populate activity
        }
    }

    public void loadUserDataFromSP(SharedPreferences sp) {
        user = new User();
        user.setId(sp.getString("uId", ""));
        user.setFirstName(sp.getString("FirstName", ""));
        user.setLastName(sp.getString("LastName", ""));
        user.setLocation(new Latlng(
                Double.parseDouble(sp.getString("Latitude", "0.0")),
                Double.parseDouble(sp.getString("Longitude", "0.0"))));
        user.setPhone(sp.getString("Phone", ""));
        user.setEmail(sp.getString("Email", ""));
    }
}
