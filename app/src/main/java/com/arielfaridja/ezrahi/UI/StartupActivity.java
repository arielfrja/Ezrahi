//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.UI;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.UI.Login.LoginActivity;
import com.arielfaridja.ezrahi.UI.Main.MainActivity;
import com.arielfaridja.ezrahi.data.DataRepo;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    DataRepo dataRepo = DataRepoFactory.getInstance();

    public StartupActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.mAuth = FirebaseAuth.getInstance();
    }

    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = this.mAuth.getCurrentUser();
        if (currentUser != null) {
            //this.startActivity(new Intent(this, MainActivity.class));
            dataRepo.user_get(currentUser.getUid(), response -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("firstName", response.getUser().getFirstName());
                intent.putExtra("lastName", response.getUser().getLastName());
                intent.putExtra("id", response.getUser().getId());
                intent.putExtra("phone", response.getUser().getPhone());
                intent.putExtra("email", response.getUser().getEmail());
                intent.putExtra("latitude", response.getUser().getLocation().getLatitude());
                intent.putExtra("longitude", response.getUser().getLocation().getLongitude());
                this.startActivity(intent);
            });
        } else {
            this.startActivity(new Intent(this, LoginActivity.class));
        }

    }
}
