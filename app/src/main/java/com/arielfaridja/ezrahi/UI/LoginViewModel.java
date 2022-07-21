//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.UI;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.arielfaridja.ezrahi.data.DataRepo;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.entities.Callback;
import com.arielfaridja.ezrahi.entities.User;

public class LoginViewModel extends ViewModel {
    private final MutableLiveData currentUser = new MutableLiveData();
    String email = "";
    String password = "";
    MutableLiveData exception = new MutableLiveData();
    DataRepo dataRepo = DataRepoFactory.getInstance();

    public LoginViewModel() {
    }

    public MutableLiveData<Exception> getException() {
        return this.exception;
    }

    private void setException(Exception exception) {
        this.exception.setValue(exception);
    }

    public LiveData<User> getCurrentUser() {
        return this.currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser.setValue(currentUser);
    }

    void loginEmailPassword() throws Exception {
        try {
            if (this.email.isEmpty()) {
                throw new Exception("email must be inserted");
            } else if (this.password.isEmpty()) {
                throw new Exception("password must be inserted");
            } else {
                this.dataRepo.auth_email_user_login(this.email, this.password, new Callback() {
                    public void onResponse(Response response) {
                        if (response.getUser() != null) {
                            LoginViewModel.this.currentUser.setValue(response.getUser());
                        } else {
                            LoginViewModel.this.setException(response.getException());
                        }

                    }
                });
            }
        } catch (Exception var2) {
            throw var2;
        }
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
