//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package com.arielfaridja.ezrahi.UI.Login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory

class LoginViewModel : ViewModel() {
    val currentUser: MutableLiveData<*> = MutableLiveData<Any?>()
    var email = ""
    var password = ""
    var exception = MutableLiveData<Exception?>()
    var dataRepo = DataRepoFactory.getInstance()


    private fun setException(exception: Exception) {
        this.exception.value = exception
    }

    @Throws(Exception::class)
    fun loginEmailPassword() {
        try {
            if (email.isEmpty()) {
                throw Exception("email must be inserted")
            } else if (password.isEmpty()) {
                throw Exception("password must be inserted")
            } else {
                dataRepo.auth_email_user_login(email, password) { response ->
                    if (response.user != null) {
                        currentUser.value = response.user
                    } else {
                        setException(response.exception)
                    }
                }
            }
        } catch (exception: Exception) {
            throw exception
        }
    }
}