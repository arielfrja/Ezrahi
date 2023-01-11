package com.arielfaridja.ezrahi.UI.Signup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.data.IDataRepo
import com.arielfaridja.ezrahi.entities.Latlng
import com.arielfaridja.ezrahi.entities.User
import java.util.regex.Pattern

private const val EMAIL_PATTERN = "^(.+)@(.+)\$"

class SignupActivityViewModel : ViewModel() {

    fun signupEmailPassword() {
        try {
            var ok = true
            val errorMessage = StringBuilder("")
            if (firstName.isEmpty()) {
                ok = false
                errorMessage.appendLine("F: $FIRST_NAME_EMPTY")
            }
            if (lastName.isEmpty()) {
                ok = false
                errorMessage.appendLine("F: $LAST_NAME_EMPTY")
            }
            if (email.isEmpty()) {
                ok = false
                errorMessage.appendLine("F: $EMAIL_EMPTY")
            } else if (!Pattern.matches(EMAIL_PATTERN, email)) {
                ok = false
                errorMessage.appendLine("F: $EMAIL_NOT_VALID")
            }
            if (password.isEmpty()) {
                ok = false
                errorMessage.appendLine("F: $PASSWORD_EMPTY")
            } else if (!passwordValidation()) {
                ok = false
                errorMessage.appendLine("F: $PASSWORD_SHORT")
            } else if (password != passwordVerification) {
                ok = false
                errorMessage.appendLine("F: $PASSWORD_DIFFERENT")
            }
            if (phone.isEmpty()) {
                ok = false
                errorMessage.appendLine("F: $PHONE_EMPTY")
            } else {
                var user = User("", firstName, lastName, phone, email, Latlng())
                IDataRepo.auth_email_user_register(user, password) { response ->
                    if (response.user != null) {
                        user = response.user
                        signedUp.value = true
                    } else if (response.exception != null) {
                        setTheException(response.exception)
                    }

                }
            }
            if (!ok)
                throw Exception(errorMessage.toString())
        } catch (exception: Exception) {
            throw exception
        }
    }

    fun setTheException(exception: Exception?) {
        this.exception.value = exception
    }

    private fun passwordValidation(): Boolean {
        var result = true
        if (password.length < 6)
            result = false
        return result
    }

    val exception = MutableLiveData<Exception>()
    private val IDataRepo: IDataRepo = DataRepoFactory.getInstance()
    var phone: String = ""
    var email: String = ""
    var lastName: String = ""
    var firstName: String = ""
    var password: String = ""
    var passwordVerification: String = ""
    var signedUp: MutableLiveData<Boolean> = MutableLiveData(false)

    companion object {
        const val EMAIL_EMPTY = "email cannot not be empty"
        const val EMAIL_NOT_VALID = "email is not valid"
        const val PASSWORD_EMPTY = "password cannot be empty"
        const val PASSWORD_SHORT = "password must have at least 6 digits"
        const val PASSWORD_DIFFERENT = "password and password verification should be the same"
        const val FIRST_NAME_EMPTY = "first name cannot be empty"
        const val LAST_NAME_EMPTY = "last name cannot be empty"
        const val PHONE_EMPTY = "phone cannot be empty"


    }

}
