//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package com.arielfaridja.ezrahi.UI.Login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.User
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity(), Observer<User?> {
    lateinit var model: LoginViewModel
    lateinit var emailPhone: TextInputLayout
    lateinit var password: TextInputLayout
    lateinit var loginButton: Button
    lateinit var loginProgressBar: FrameLayout
    var user: User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this).get(LoginViewModel::class.java)
        this.setContentView(R.layout.activity_login)
        emailPhone = findViewById<View>(R.id.email_or_phone) as TextInputLayout
        password = findViewById<View>(R.id.password) as TextInputLayout
        loginButton = findViewById<View>(R.id.loginButton) as Button
        loginProgressBar = findViewById<View>(R.id.loginProgressBar) as FrameLayout
        emailPhone.editText!!.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (emailPhone.error != null) {
                    emailPhone.error = null
                }
                model.email = (charSequence.toString())
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
        model.currentUser.observe(this, (this as Observer<Any>))
        model.exception.observe(this) { e ->
            if (e != null) {
                Toast.makeText(this@LoginActivity.applicationContext, e.message, Toast.LENGTH_LONG)
                    .show()
            }
            loginProgressBar.visibility = View.INVISIBLE
        }
    }

    override fun onChanged(user: User?) {
        if (user != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("firstName", user.firstName)
            intent.putExtra("lastName", user.lastName)
            intent.putExtra("id", user.id)
            intent.putExtra("phone", user.phone)
            intent.putExtra("email", user.email)
            intent.putExtra("latitude", user.location.latitude)
            intent.putExtra("longitude", user.location.longitude)
            this.startActivity(intent)
        }
    }
}