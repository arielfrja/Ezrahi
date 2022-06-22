//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.UI;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.entities.User;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity implements Observer<User> {
    LoginViewModel model;
    TextInputLayout emailPhone;
    TextInputLayout password;
    Button loginButton;
    FrameLayout loginProgressBar;
    User user;

    public LoginActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.model = (LoginViewModel) (new ViewModelProvider(this)).get(LoginViewModel.class);
        this.setContentView(R.layout.activity_login);
        this.emailPhone = (TextInputLayout) this.findViewById(R.id.email_or_phone);
        this.password = (TextInputLayout) this.findViewById(R.id.password);
        this.loginButton = (Button) this.findViewById(R.id.loginButton);
        this.loginProgressBar = (FrameLayout) this.findViewById(R.id.loginProgressBar);
        this.emailPhone.getEditText().addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (LoginActivity.this.emailPhone.getError() != null) {
                    LoginActivity.this.emailPhone.setError((CharSequence) null);
                }

                LoginActivity.this.model.setEmail(charSequence.toString());
            }

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
            }
        });
        this.password.getEditText().addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (LoginActivity.this.password.getError() != null) {
                    LoginActivity.this.password.setError((CharSequence) null);
                }

                LoginActivity.this.model.setPassword(charSequence.toString());
            }

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
            }
        });
        this.loginButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                try {
                    LoginActivity.this.loginProgressBar.setVisibility(0);
                    LoginActivity.this.model.loginEmailPassword();
                } catch (Exception var3) {
                    if (var3.getMessage().contains("email")) {
                        LoginActivity.this.emailPhone.setError(var3.getMessage());
                    } else if (var3.getMessage().contains("password")) {
                        LoginActivity.this.password.setError(var3.getMessage());
                    }

                    LoginActivity.this.loginProgressBar.setVisibility(4);
                }

            }
        });
        this.model.getCurrentUser().observe(this, this);
        this.model.getException().observe(this, new Observer<Exception>() {
            public void onChanged(Exception e) {
                Toast.makeText(LoginActivity.this.getApplicationContext(), e.getMessage(), 1).show();
                LoginActivity.this.loginProgressBar.setVisibility(4);
            }
        });
    }

    public void onChanged(User user) {
        if (user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("firstName", user.getFirstName());
            intent.putExtra("lastName", user.getLastName());
            intent.putExtra("id", user.getId());
            intent.putExtra("phone", user.getPhone());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("latitude", user.getLocation().getLatitude());
            intent.putExtra("longitude", user.getLocation().getLongitude());
            this.startActivity(intent);
        }

    }
}
