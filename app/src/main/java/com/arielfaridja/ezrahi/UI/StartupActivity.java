package com.arielfaridja.ezrahi.UI;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.data.DataRepo;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.entities.User;

public class StartupActivity extends AppCompatActivity {
    DataRepo dataRepo = DataRepoFactory.getInstance();

    public StartupActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_startup);
    }

    protected void onStart() {
        super.onStart();
        StartupActivity current = this;
        dataRepo.currentUser_get(response -> {
            User user = new User();
            if (response.getUser() != null) {
                user.copy(response.getUser());
                Intent intent = new Intent(current, MainActivity.class);
                putExtras(user, intent);
                startActivity(intent);
            } else {
                startActivity(new Intent(current, LoginActivity.class));
            }
        });
    }

    private void putExtras(User user, Intent intent) {
        intent.putExtra("id",user.getId());
        intent.putExtra("firstName", user.getFirstName());
        intent.putExtra("lastName", user.getLastName());
        intent.putExtra("phone", user.getPhone());
        intent.putExtra("email", user.getEmail());
        intent.putExtra("latitude", user.getLocation().getLatitude());
        intent.putExtra("longitude", user.getLocation().getLongitude());
    }
}
