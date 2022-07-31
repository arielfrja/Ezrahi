package com.arielfaridja.ezrahi.UI.Main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.arielfaridja.ezrahi.data.DataRepo;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;

public class MainActivityViewModel extends ViewModel {


    private User user;
    Context context;
    DataRepo dataRepo = DataRepoFactory.getInstance();
    Intent intent;

    MutableLiveData<Boolean> isSignIn = new MutableLiveData<>(null);
    private Activity activity;

    public MainActivityViewModel() {
        super();
        getIsSignIn().setValue(dataRepo.user_isSignedIn());

    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public MutableLiveData<Boolean> getIsSignIn() {
        return isSignIn;
    }

    public void setIsSignIn(MutableLiveData<Boolean> isSignIn) {
        this.isSignIn = isSignIn;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void isUserSignedIn() {
        if (!dataRepo.user_isSignedIn())
            isSignIn.setValue(false);
        else
            isSignIn.setValue(true);
    }

    public void loadUserDatafromSP(SharedPreferences sp) {
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

    public void loadActivityDatafromSP(SharedPreferences sp) {
        activity = new Activity();
    }
}
