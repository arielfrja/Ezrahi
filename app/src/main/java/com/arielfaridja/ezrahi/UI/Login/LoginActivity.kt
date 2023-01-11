//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package com.arielfaridja.ezrahi.UI.Login

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.UI.Signup.SignupActivity
import com.arielfaridja.ezrahi.entities.User
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity(), Observer<User?> {
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var model: LoginViewModel
    lateinit var emailPhone: TextInputLayout
    lateinit var password: TextInputLayout
    lateinit var loginButton: Button
    lateinit var loginProgressBar: FrameLayout
    lateinit var dontHaveAnAccount: Button
    var user: User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this).get(LoginViewModel::class.java)
        sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE)


        this.setContentView(R.layout.activity_login)
        emailPhone = findViewById(R.id.email_or_phone)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        loginProgressBar = findViewById(R.id.loginProgressBar)
        dontHaveAnAccount = findViewById(R.id.dontHaveAnAccount)
        emailPhone.editText!!.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (emailPhone.error != null) {
                    emailPhone.error = null
                }
                model.email = (charSequence.toString()).lowercase()
            }

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}
        })
        password.editText!!.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (password.error != null) {
                    password.error = null
                }
                model.password = charSequence.toString()
            }

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}
        })
        loginButton.setOnClickListener {
            try {
                loginProgressBar.visibility = View.VISIBLE
                model.loginEmailPassword()
            } catch (var3: Exception) {
                if (var3.message!!.contains("email")) {
                    emailPhone.error = var3.message
                } else if (var3.message!!.contains("password")) {
                    password.error = var3.message
                }
                loginProgressBar.visibility = View.INVISIBLE
            }
        }
        dontHaveAnAccount.setOnClickListener {
            var intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
        model.currentUser.observe(this, (this as Observer<Any>))
        model.exception.observe(this) { e ->
            if (e != null) {
                Toast.makeText(this@LoginActivity.applicationContext, e.message, Toast.LENGTH_LONG)
                    .show()
                var dialog = AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_round_warning_48)
                    .setTitle("We run into a problem")
                    .setMessage(e.message)
                    .setNeutralButton(
                        "ok, I got it",
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            dialogInterface.dismiss()
                        })
                dialog.create().show()

            }
            loginProgressBar.visibility = View.INVISIBLE
        }
    }

    override fun onChanged(user: User?) {
        if (user != null) {
            //putUserToSP(user)
            val intent = Intent(this, MainActivity::class.java)
//            intent.putExtra("firstName", user.firstName)
//            intent.putExtra("lastName", user.lastName)
//            intent.putExtra("id", user.id)
//            intent.putExtra("phone", user.phone)
//            intent.putExtra("email", user.email)
//            intent.putExtra("latitude", user.location.latitude)
//            intent.putExtra("longitude", user.location.longitude)
            this.startActivity(intent)
        }
    }


    private fun putUserToSP(u: User) {
        val editor = sharedPreferences.edit()
        for (field in u.toHashMap().keys) {
            editor.putString(
                field as String,
                u.toHashMap()[field].toString()
            )
        }
        editor.commit()
    }
}