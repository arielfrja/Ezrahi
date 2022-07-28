package com.arielfaridja.ezrahi.UI.Main;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.ViewModel;

import com.arielfaridja.ezrahi.data.DataRepo;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;

public class MainActivityViewModel extends ViewModel {


    private User user;
    Context context;
    DataRepo dataRepo = DataRepoFactory.getInstance();
    Intent intent;

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
}
