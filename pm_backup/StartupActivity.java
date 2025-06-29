//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.UI.Login.LoginActivity;
import com.arielfaridja.ezrahi.UI.Main.MainActivity;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.data.IDataRepo;
import com.arielfaridja.ezrahi.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    IDataRepo IDataRepo = DataRepoFactory.getInstance(getApplicationContext());
    SharedPreferences sharedPreferences;

    public StartupActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE);
        this.mAuth = FirebaseAuth.getInstance();
    }

    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = this.mAuth.getCurrentUser();
        if (currentUser != null) {
            IDataRepo.user_get(currentUser.getUid(), response -> {
                User u = response.getUser();
                //putUserToSP(u);
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("firstName", u.getFirstName());
                intent.putExtra("lastName", u.getLastName());
                intent.putExtra("id", u.getId());
                intent.putExtra("phone", u.getPhone());
                intent.putExtra("email", u.getEmail());
                intent.putExtra("latitude", u.getLocation().getLatitude());
                intent.putExtra("longitude", u.getLocation().getLongitude());
                //this.startActivity(intent);
            });
        } else {
            this.startActivity(new Intent(this, LoginActivity.class));
        }

    }

//    private void putUserToSP(User u) {
//        for (Object field: u.toHashMap().keySet()
//             ) {
//            sharedPreferences.edit().putString((String) field,
//                    (String)u.toHashMap().get(field).toString());
//        }
//        sharedPreferences.edit().commit();
//
//    }
}
